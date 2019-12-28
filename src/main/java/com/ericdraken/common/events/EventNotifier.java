/*
 * Copyright (c) 2019. Eric Draken - ericdraken.com
 */

package com.ericdraken.common.events;

import com.ericdraken.common.executors.NamedThreadFactory;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.ericdraken.common.exceptions.ExceptionUtils.getMessage;

public final class EventNotifier implements EventSubscriberMethods
{
	private final Set<EventSubscriber> subscribers = new HashSet<>();

	private final ReentrantLock lock = new ReentrantLock();

	// Effectively a new cachedThreadPoolExecutor with a name
	private final ExecutorService executorService = new ThreadPoolExecutor(
		0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
		new SynchronousQueue<>(), new NamedThreadFactory( "events" )
	);

	private static final EventNotifier instance = new EventNotifier();

	private static final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	//////

	@VisibleForTesting
	EventNotifier()
	{
	}

	public static EventNotifier getInstance()
	{
		return instance;
	}

	public EventNotifier addAndInitSubscriber( @Nonnull EventSubscriber subscriber )
	{
		lock.lock();
		try
		{
			if ( ! subscribers.contains( subscriber ) )
			{
				subscribers.add( subscriber );
				logger.info( "Initializing {}", subscriber.getDescription() );
				subscriber.init();
			}
			else
			{
				logger.warn( "Subscriber {} already exists", subscriber.getDescription() );
			}
		}
		catch ( Exception e )
		{
			logger.warn( getMessage( e ) );
		}
		finally
		{
			lock.unlock();
		}
		return this;
	}

	public EventNotifier shutdownAndRemoveSubscriber( @Nonnull EventSubscriber subscriber )
	{
		lock.lock();
		try
		{
			logger.info( "Shutting down {}", subscriber.getClass().getSimpleName() );
			subscriber.shutdown();
			subscribers.remove( subscriber );
		}
		catch ( Exception e )
		{
			logger.warn( getMessage( e ) );
		}
		finally
		{
			lock.unlock();
		}
		return this;
	}

	public Map<String, Boolean> getSubscribers()
	{
		Map<String, Boolean> list = new LinkedHashMap<>();
		subscribers.forEach( s -> list.put( s.getDescription(), s.isReady() ) );
		return list;
	}

	public void shutdown()
	{
		lock.lock();
		try
		{
			for ( EventSubscriberInit s : subscribers )
			{
				try
				{
					logger.info( "Shutting down {}", s.getClass().getSimpleName() );
					s.shutdown();
				}
				catch ( Exception e )
				{
					logger.warn( getMessage( e ) );
				}
			}
			subscribers.clear();

			// Graceful shutdown
			try
			{
				logger.debug( "Attempting to shutdown events executor pool" );
				executorService.shutdown();
				executorService.awaitTermination( 10, TimeUnit.SECONDS );
			}
			catch ( InterruptedException e )
			{
				logger.warn( "Events shutdown interrupted" );
				Thread.currentThread().interrupt();
			}
			finally
			{
				if ( !executorService.isTerminated() )
				{
					logger.warn( "Forcibly canceling unfinished event tasks" );
				}
				executorService.shutdownNow();
				logger.debug( "Events shutdown finished" );
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	//////

	private final EventSubscriberMethods proxy = (EventSubscriberMethods) Proxy.newProxyInstance(
		EventSubscriberMethods.class.getClassLoader(),
		new Class[]{EventSubscriberMethods.class},
		( proxy0, method, methodArgs ) -> {
			for ( EventSubscriber s : subscribers )
			{
				executorService.submit( () -> {
					try
					{
						method.invoke( s, methodArgs );
					}
					catch ( Exception e )
					{
						logger.error( getMessage( e ) );
					}
				} );
			}
			return true;
		}
	);

	@Override
	public void serviceTrouble()
	{
		proxy.serviceTrouble();
	}

	@Override
	public void networkTrouble()
	{
		proxy.networkTrouble();
	}

	@Override
	public void systemTrouble()
	{
		proxy.systemTrouble();
	}

	@Override
	public void updateApiRate( double rate )
	{
		proxy.updateApiRate( rate );
	}

	@Override
	public void uploadEvent()
	{
		proxy.uploadEvent();
	}

	@Override
	public void dbOperationEvent( boolean res )
	{
		proxy.dbOperationEvent( res );
	}

	@Override
	public void heartbeat()
	{
		proxy.heartbeat();
	}

	@Override
	public void resetState()
	{
		proxy.resetState();
	}
}
