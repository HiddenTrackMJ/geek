package org.seekloud.geek.utils

import net.sf.ehcache.{CacheManager, Ehcache, Element}

import scala.concurrent.{ExecutionContext, Future, Promise}


object EhCacheApi {

  System.setProperty("net.sf.ehcache.enableShutdownHook", "true")

  private[this] val defaultClassLoader = Thread.currentThread().getContextClassLoader
  private[this] val configResource = defaultClassLoader.getResource("ehcache-default.xml")
  private[this] val manager = CacheManager.create(configResource)


  def createCache[V](
    name: String,
    timeToIdle: Int = 120,
    timeToLife: Int = 120): EhCacheApi[V] = {

    manager.addCache(name)
    new EhCacheApi[V](manager.getEhcache(name), timeToIdle, timeToLife)
  }


}

/**
  * User: Taoz
  * Date: 12/10/2016
  * Time: 10:34 AM
  */
class EhCacheApi[V](
  ehCache: Ehcache,
  timeToIdle: Int = 120,
  timeToLife: Int = 120
) extends Cache[V] {

  /**
    * Returns either the cached Future for the given key or evaluates the given value generating
    * function producing a `Future[V]`.
    */
  override def apply(key: Any, genValue: () => Future[V])(implicit ec: ExecutionContext): Future[V] = {


    get(key) match {
      case Some(v) => v
      case None =>
        val promise = Promise[V]()
        val elem = new Element(key, promise.future)
        elem.setTimeToIdle(timeToIdle)
        elem.setTimeToLive(timeToLife)
        ehCache.putIfAbsent(elem, true) match {
          case null =>
            val f = genValue()
            f.onComplete { value =>
              promise.complete(value)
              if (value.isFailure) ehCache.remove(key)
            }
            f
          case element => element.getObjectValue.asInstanceOf[Future[V]]
        }
    }
  }

  /**
    * Retrieves the future instance that is currently in the cache for the given key.
    * Returns None if the key has no corresponding cache entry.
    */
  override def get(key: Any): Option[Future[V]] = {
    val element = ehCache.get(key)
    if (element != null) {
      Some(element.getObjectValue.asInstanceOf[Future[V]])
    } else None
  }

  /**
    * Removes the cache item for the given key. Returns the removed item if it was found (and removed).
    */
  override def remove(key: Any): Option[Future[V]] = {
    val element = ehCache.get(key)
    if (element != null) {
      ehCache.remove(key, true)
      Some(element.getObjectValue.asInstanceOf[Future[V]])
    }
    else None
  }

  /**
    * Clears the cache by removing all entries.
    */
  override def clear(): Unit = ehCache.removeAll(true)


  import collection.JavaConverters._

  /**
    * Returns the set of keys in the cache, in no particular order
    * Should return in roughly constant time.
    * Note that this number might not reflect the exact keys of active, unexpired
    * cache entries, since expired entries are only evicted upon next access
    * (or by being thrown out by a capacity constraint).
    */
  override def keys: Set[Any] = ehCache.getKeysWithExpiryCheck.asScala.toSet

  /**
    * Returns a snapshot view of the keys as an iterator, traversing the keys from the least likely
    * to be retained to the most likely.  Note that this is not constant time.
    *
    * @param limit No more than limit keys will be returned
    */
  override def ascendingKeys(limit: Option[Int]): Iterator[Any] = {
    throw new UnsupportedOperationException()
  }

  /**
    * Returns the upper bound for the number of currently cached entries.
    * Note that this number might not reflect the exact number of active, unexpired
    * cache entries, since expired entries are only evicted upon next access
    * (or by being thrown out by a capacity constraint).
    */
  override def size: Int = ehCache.getSize
}
