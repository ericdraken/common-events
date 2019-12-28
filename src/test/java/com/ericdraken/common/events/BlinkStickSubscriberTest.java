/*
 * Copyright (c) 2019. Eric Draken - ericdraken.com
 */

package com.ericdraken.common.events;

import com.ericdraken.blinkstick.BlinkStickException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BlinkStickSubscriberTest
{
	private static BlinkStickSubscriber bss = null;

	@BeforeAll
	static void init() throws BlinkStickException
	{
		bss = new BlinkStickSubscriber();
		bss.init();

		assumeTrue( bss.isReady() );
	}

	@AfterAll
	static void shutdown() throws Exception
	{
		bss.shutdown();
	}

	// Add a delay between each test
	@AfterEach
	void delay() throws InterruptedException
	{
		Thread.sleep( 3000 ); // NOSONAR
	}

	@BeforeEach
	void reset() {
		bss.resetState();
	}

	@Test
	void isReady()
	{
		assertTrue( bss.isReady() );
	}

	@Test
	void serviceTrouble()
	{
		System.out.println( "Service trouble" );
		bss.serviceTrouble();
	}

	@Test
	void networkTrouble()
	{
		System.out.println( "Network trouble" );
		bss.networkTrouble();
	}

	@Test
	void systemTrouble()
	{
		System.out.println( "System trouble" );
		bss.systemTrouble();
	}

	@ParameterizedTest
	@ValueSource(doubles = { -0.25, 0, 0.25, 0.5, 0.75, 1, 1.25 })
	void updateApiRate( double rate )
	{
		bss.updateApiRate( rate );
	}

	@Test
	void crossFadeTest() throws InterruptedException
	{
		bss.updateApiRate( 1.0 );
		Thread.sleep( 2500 );
		bss.updateApiRate( 0.0 );
		Thread.sleep( 2500 );
		bss.updateApiRate( 1.0 );
	}

	@RepeatedTest(5)
	void uploadEvent()
	{
		bss.uploadEvent();
	}

	@RepeatedTest(5)
	void dbOperationEvent_false()
	{
		bss.dbOperationEvent( false );
	}

	@RepeatedTest(5)
	void dbOperationEvent_true()
	{
		bss.dbOperationEvent( true );
	}

	@RepeatedTest(5)
	void heartbeat_fromOff()
	{
		bss.heartbeat();
	}

	@Test
	void heartbeat_fromYellow() throws InterruptedException
	{
		bss.updateApiRate( 0.5 );

		for ( int i = 0; i < 10; i++ )
		{
			bss.heartbeat();
			Thread.sleep( 500 );
		}
	}

	@Test
	void resetState()
	{
		bss.resetState();
	}
}