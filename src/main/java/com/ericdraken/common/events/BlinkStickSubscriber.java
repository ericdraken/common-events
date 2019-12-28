/*
 * Copyright (c) 2019. Eric Draken - ericdraken.com
 */

package com.ericdraken.common.events;

import com.ericdraken.blinkstick.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.locks.ReentrantLock;

import static com.ericdraken.common.exceptions.ExceptionUtils.getMessage;
import static com.ericdraken.common.threads.Sleep.parkThread;

public class BlinkStickSubscriber implements EventSubscriber
{
	private Usb2 usbManager = null;

	private BlinkStick blinkStick = null;

	private static final double DEFAULT_BRIGHTNESS = 0.5D;

	private static final double DIM_BRIGHTNESS = 0.2D;

	private static final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	private final ReentrantLock lock = new ReentrantLock();

	@Override
	public String getDescription()
	{
		return "BlinkStick";
	}

	@Override
	public boolean isReady()
	{
		return blinkStick != null;
	}

	@Override
	public void init() throws BlinkStickException
	{
		lock.lock();
		try  // NOSONAR
		{
			usbManager = new Usb2();

			// Hold a reference to the first BlinkStick
			usbManager.findFirstBlinkStick( true ).ifPresent( b -> blinkStick = b );

			// Indicate the BlinkStick is present
			if ( blinkStick != null )
			{
				new Effects( blinkStick ).strobe( Colors.Blue, Runtime.getRuntime().availableProcessors(), 500 );
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public void shutdown() throws Exception
	{
		lock.lock();
		try
		{
			// Turn off the BlinkStick
			if ( blinkStick != null )
			{
				try
				{
					blinkStick.turnOff();
					blinkStick.close();
				}
				finally
				{
					blinkStick = null;
				}
			}

			// Shutdown the manager
			if ( usbManager != null )
			{
				try
				{
					usbManager.close();
				}
				finally
				{
					usbManager = null;
				}
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	private Color getCurrColor()
	{
		try
		{
			return blinkStick.getColorObj();
		}
		catch ( BlinkStickException e )
		{
			return new Color(0,0,0);
		}
	}

	////

	@Override
	public void serviceTrouble()
	{
		tryLockRun( () -> {
			Pattern pattern = new Pattern();
			for ( int i = 0; i < 5; i++ )
			{
				pattern.addColorAndDuration( Colors.Yellow, 500 );
				pattern.addColorAndDuration( Colors.Black, 200 );
			}
			blinkStick.setBlinkPattern( pattern );
		} );
	}

	@Override
	public void networkTrouble()
	{
		tryLockRun( () -> {
			Pattern pattern = new Pattern();
			for ( int i = 0; i < 5; i++ )
			{
				pattern.addColorAndDuration( Colors.Red, 500 );
				pattern.addColorAndDuration( Colors.Black, 200 );
			}
			blinkStick.setBlinkPattern( pattern );
		} );
	}

	@Override
	public void systemTrouble()
	{
		tryLockRun( () -> {
			Pattern pattern = new Pattern();
			for ( int i = 0; i < 20; i++ )
			{
				pattern.addColorAndDuration( Colors.Red, 200 );
				pattern.addColorAndDuration( Colors.Black, 200 );
			}
			blinkStick.setBlinkPattern( pattern );
		} );
	}

	@Override
	public void updateApiRate( final double apiRate )
	{
		tryLockRun( () -> {
			Color currColor = getCurrColor();

			// Pulse the brightness
			if ( ! currColor.equals( Colors.Black ) )
			{
				blinkStick.setColor( Luminance.setBrightness( currColor, DEFAULT_BRIGHTNESS * 0.6 ) );
				parkThread( 50 );
			}

			// Sanity check
			double rate = apiRate;
			rate = Math.max( rate, 0.0 );
			rate = Math.min( rate, 1.0 );

			// Clip the top end
			rate = rate >= 0.95 ? 1.0 : rate;

			// Prevent deep red by preventing 0.0
			rate = (rate * 0.94) + 0.06;    // Shrink and shift the range

			Color endColor = Effects.colorFromTrafficLightGradient( rate, DEFAULT_BRIGHTNESS );
			crossFade( currColor, endColor, 2000 );
		} );
	}

	@Override
	public void uploadEvent()
	{
		tryLockRun( () -> {
			Pattern pattern = new Pattern();
			for ( int i = 4; i >= 1; i-- )
			{
				pattern.addColorAndDuration( Luminance.setBrightness( Colors.Blue, DIM_BRIGHTNESS ), 10 + (i*15) );
				pattern.addColorAndDuration( Luminance.setBrightness( Colors.Yellow, DIM_BRIGHTNESS ), 10 + (i*15) );
			}
			pattern.addColorAndDuration( getCurrColor(), 0 );
			blinkStick.setBlinkPattern( pattern );
		} );
	}

	@Override
	public void dbOperationEvent( boolean res )
	{
		tryLockRun( () -> {
			Pattern pattern = new Pattern()
				.addColorAndDuration( Colors.Black, 50 )
				.addColorAndDuration(
					Luminance.setBrightness( res ? Colors.DeepskyBlue : Colors.Darkmagenta, DIM_BRIGHTNESS ), 50 )
				.addColorAndDuration( getCurrColor(), 0 );
			blinkStick.setBlinkPattern( pattern );
		} );
	}

	@Override
	public void heartbeat()
	{
		tryLockRun( () -> {
			Color currColor = getCurrColor();
			Pattern pattern = new Pattern();
			for ( int i = 0; i < 2; i++ )
			{
				if ( ! currColor.equals( Colors.Black ) )
				{
					// Pulse the brightness
					pattern.addColorAndDuration( Luminance.setBrightness( currColor, DIM_BRIGHTNESS ), 150 );
					pattern.addColorAndDuration( currColor, 150 );
				}
				else
				{
					// Pulse a color on and off
					pattern.addColorAndDuration( Luminance.setBrightness( Colors.Green, DIM_BRIGHTNESS ), 150 );
					pattern.addColorAndDuration( Colors.Black, 150 );
				}
			}
			blinkStick.setBlinkPattern( pattern );
		} );
	}

	@Override
	public void resetState()
	{
		lockRun( ()-> blinkStick.turnOff() );
	}

	private void tryLockRun( CheckedRunner runner )
	{
		if ( blinkStick != null && lock.tryLock() )
		{
			try
			{
				runner.run();
			}
			catch ( InterruptedException e )
			{
				Thread.currentThread().interrupt();
				try
				{
					blinkStick.turnOff();
				}
				catch ( BlinkStickException e1 )
				{
					logger.warn( getMessage( e ) );
				}
			}
			catch ( BlinkStickException e )
			{
				logger.warn( getMessage( e ) );
			}
			catch ( Exception e )
			{
				logger.error( getMessage( e ) );
			}
			finally
			{
				lock.unlock();
			}
		}
	}

	private void lockRun( CheckedRunner runner )
	{
		if ( blinkStick != null )
		{
			lock.lock();
			try
			{
				runner.run();
			}
			catch ( Exception e )
			{
				logger.warn( getMessage( e ) );
			}
			finally
			{
				lock.unlock();
			}
		}
	}

	private interface CheckedRunner
	{
		void run() throws BlinkStickException, InterruptedException;
	}

	/**
	 * Cross fade from one color to the next using parkThread(), not sleep()
	 *
	 * @param before     From color
	 * @param after      To color
	 * @param durationMs Transition duration
	 * @throws BlinkStickException Exception
	 */
	private void crossFade( Color before, Color after, int durationMs ) throws BlinkStickException
	{
		final int steps = Pattern.MAX_PATTERN_COUNT;

		int stepDelayMs = Math.round( durationMs / (float)steps );

		float rb = before.getRed();
		float gb = before.getGreen();
		float bb = before.getBlue();

		int ra = after.getRed();
		int ga = after.getGreen();
		int ba = after.getBlue();

		float rs = (ra - rb) / steps;
		float gs = (ga - gb) / steps;
		float bs = (ba - bb) / steps;

		Pattern pattern = new Pattern();
		for ( int i = 0; i < steps; i++ )
		{
			rb += rs;
			gb += gs;
			bb += bs;

			pattern.addColorAndDuration( new Color(
				Math.min( Math.max( Math.round( rb ), 0 ), 255 ),
				Math.min( Math.max( Math.round( gb ), 0 ), 255 ),
				Math.min( Math.max( Math.round( bb ), 0 ), 255 )
			), stepDelayMs );
		}
		blinkStick.setBlinkPattern( pattern );
	}
}
