/*
 * Copyright (c) 2019. Eric Draken - ericdraken.com
 */

package com.ericdraken.common.events;

import com.google.common.util.concurrent.AtomicDouble;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventNotifierTest
{
	@Test
	void addSameMultiple()
	{
		final AtomicInteger initCount = new AtomicInteger();

		EventNotifier n = new EventNotifier();

		EventSubscriber s = new EventSubscriber()
		{
			@Override
			public void init() throws Exception
			{
				System.out.println( "init" );
				initCount.incrementAndGet();
			}

			@Override
			public void shutdown() throws Exception
			{
			}

			@Override
			public String getDescription()
			{
				return "dummy";
			}

			@Override
			public boolean isReady()
			{
				return false;
			}
		};

		n
			.addAndInitSubscriber( s )
			.addAndInitSubscriber( s )
			.addAndInitSubscriber( s )
			.addAndInitSubscriber( s );

		assertEquals( 1, initCount.get() );
	}

	@Test
	void heartbeat() throws InterruptedException
	{
		final AtomicBoolean didRun = new AtomicBoolean();

		EventNotifier n = new EventNotifier();
		n.addAndInitSubscriber( new EventSubscriber()
		{
			@Override
			public void init() throws Exception
			{
			}

			@Override
			public void shutdown() throws Exception
			{
			}

			@Override
			public String getDescription()
			{
				return "";
			}

			@Override
			public boolean isReady()
			{
				return true;
			}

			@Override
			public void heartbeat()
			{
				didRun.set( true );
			}
		} );

		n.heartbeat();

		Thread.sleep( 500 );

		assertTrue( didRun.get() );
	}

	@Test
	void updateApiRate() throws InterruptedException
	{
		final AtomicDouble atomicRate = new AtomicDouble();

		EventNotifier n = new EventNotifier();
		n.addAndInitSubscriber( new EventSubscriber()
		{
			@Override
			public void init() throws Exception
			{
			}

			@Override
			public void shutdown() throws Exception
			{
			}

			@Override
			public String getDescription()
			{
				return "";
			}

			@Override
			public boolean isReady()
			{
				return true;
			}

			@Override
			public void updateApiRate( double rate )
			{
				atomicRate.set( rate );
			}
		} );

		n.updateApiRate( 10.0 );

		Thread.sleep( 500 );

		assertTrue( atomicRate.get() > 9.9 );
	}
}