package com.ryanm.droid.rugl.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.util.Log;

import com.ryanm.preflect.annote.Category;
import com.ryanm.preflect.annote.Summary;
import com.ryanm.preflect.annote.Variable;

/**
 * A convenience class to do code profiling.
 * 
 * @author Keith Woodward
 */
@Variable( "Code Timer" )
@Summary( "Code profiling tool" )
public class CodeTimer
{
	private static DecimalFormat fourDP = new DecimalFormat( "0.####" );

	private static DecimalFormat percent = new DecimalFormat( "0.#%" );

	/**
	 * How long a call to {@link #tick(String)} takes
	 */
	private static long SELF_TIME;

	private static final long NANOS_IN_A_SECOND = 1000000000;

	private static final long NANOS_IN_A_MILLISECOND = 1000000;

	private static final String LOGTAG = "CodeTimer";

	static
	{
		recalibrate( 10000 );
	}

	private final StringBuilder buff = new StringBuilder();

	/**
	 * The name of this {@link CodeTimer}, to identify the output
	 */
	public final String name;

	/**
	 * Names of intervals
	 */
	private String[] intervalNames = new String[5];

	/**
	 * Sum of interval durations
	 */
	private long[] intervalDurationSums = new long[intervalNames.length];

	/**
	 * Minimum interval durations
	 */
	private long[] minIntervalDurations = new long[intervalNames.length];

	/**
	 * Maximum interval durations
	 */
	private long[] maxIntervalDurations = new long[intervalNames.length];

	/**
	 * The index of the current interval
	 */
	private int intervalIndex;

	/**
	 * The time of the last call to {@link #tick(String)}
	 */
	private long lastClickTime;

	/**
	 * Count of timing periods
	 */
	private int periodCount = 0;

	private boolean periodStarted = false;

	private long periodStartTime;

	/**
	 * Sum of period durations
	 */
	private long periodDurationSum = 0;

	/**
	 * Minimum period duration
	 */
	private long minPeriodDuration = Long.MAX_VALUE;

	/**
	 * Maximum period duration
	 */
	private long maxPerdiodDuration = -1;

	/**
	 * Defaults to <code>true</code>
	 */
	@Variable( "Enabled" )
	public boolean enabled = true;

	private long lastPrintOutNanos = System.nanoTime();

	/**
	 * The number of seconds between prints, defaults to 5
	 */
	@Variable( "Log frequency" )
	@Summary( "Seconds between output logging" )
	public int logFrequencySeconds = 5;

	/**
	 * The level of information printed for the total time spent in a profiling
	 * period
	 */
	@Variable( "Period" )
	@Category( "Output" )
	public Output period;

	/**
	 * The level of information printed for time taken in intervals
	 */
	@Variable( "Interval" )
	@Category( "Output" )
	public Output interval;

	/**
	 * @param name
	 *           A name for this {@link CodeTimer}, so as to identify the output
	 * @param period
	 *           output for profiling period duration. May not be null
	 * @param interval
	 *           output for interval durations, May not be null
	 */
	public CodeTimer( final String name, final Output period,
			final Output interval )
	{
		this.name = name;
		this.period = period;
		this.interval = interval;

		Arrays.fill( minIntervalDurations, Long.MAX_VALUE );
		Arrays.fill( maxIntervalDurations, -1 );
	}

	/**
	 * Call to start a profiling period, or to start an interval in an open
	 * period
	 * 
	 * @param name
	 *           A helpful name for this interval. Makes it easy to find what bit
	 *           of code you're measuring
	 */
	public void tick( final String name )
	{
		if( enabled )
		{
			final long clickTime = System.nanoTime();

			if( !periodStarted )
			{
				periodStarted = true;
				periodStartTime = clickTime;

				intervalNames[ 0 ] = name;
			}
			else
			{
				final long duration = clickTime - lastClickTime;
				intervalDurationSums[ intervalIndex ] += duration;
				if( duration < minIntervalDurations[ intervalIndex ] )
				{
					minIntervalDurations[ intervalIndex ] = duration;
				}
				else if( duration > maxIntervalDurations[ intervalIndex ] )
				{
					maxIntervalDurations[ intervalIndex ] = duration;
				}

				intervalIndex++;

				if( intervalIndex >= intervalNames.length )
				{
					intervalNames = ArrayUtil.grow( intervalNames );
					intervalDurationSums = ArrayUtil.grow( intervalDurationSums );
					minIntervalDurations = ArrayUtil.grow( minIntervalDurations );
					maxIntervalDurations = ArrayUtil.grow( maxIntervalDurations );
				}

				intervalNames[ intervalIndex ] = name;
			}

			lastClickTime = clickTime;
		}
	}

	/**
	 * Call to end a profiling period, and print the results if
	 * {@link #logFrequencySeconds} have passed since we last printed
	 */
	public void lastTick()
	{
		lastTick( System.nanoTime() - lastPrintOutNanos > logFrequencySeconds
				* NANOS_IN_A_SECOND );
	}

	/**
	 * Call to end a profiling period
	 * 
	 * @param print
	 *           <code>true</code> to print results, <code>false</code> not to
	 */
	public void lastTick( final boolean print )
	{
		if( enabled )
		{
			final long clickTime = System.nanoTime();

			final long intervalDuration = clickTime - lastClickTime;
			intervalDurationSums[ intervalIndex ] += intervalDuration;
			if( intervalDuration < minIntervalDurations[ intervalIndex ] )
			{
				minIntervalDurations[ intervalIndex ] = intervalDuration;
			}
			else if( intervalDuration > maxIntervalDurations[ intervalIndex ] )
			{
				maxIntervalDurations[ intervalIndex ] = intervalDuration;
			}

			intervalIndex = 0;

			final long periodDuration = clickTime - periodStartTime;
			periodDurationSum += periodDuration;
			if( periodDuration < minPeriodDuration )
			{
				minPeriodDuration = periodDuration;
			}
			else if( periodDuration > maxPerdiodDuration )
			{
				maxPerdiodDuration = periodDuration;
			}

			periodCount++;

			periodStarted = false;

			if( print )
			{
				buff.append( name ).append( " period \tmin=" )
						.append( period.format( minPeriodDuration, 1 ) )
						.append( "\tmean=" )
						.append( period.format( periodDurationSum, periodCount ) )
						.append( "\tmax=" )
						.append( period.format( maxPerdiodDuration, 1 ) );
				Log.i( LOGTAG, buff.toString() );
				buff.delete( 0, buff.length() );

				final long perDur = periodDurationSum / periodCount;

				for( int i = 0; i < intervalNames.length
						&& intervalNames[ i ] != null; i++ )
				{
					final float intDur = intervalDurationSums[ i ] / periodCount;
					final float p = intDur / perDur;
					buff.append( "\t" )
							.append( intervalNames[ i ] )
							.append( "\t" + percent.format( p ) )
							.append( "\t" )
							.append( interval.format( minIntervalDurations[ i ], 1 ) )
							.append( "\t" )
							.append(
									interval.format( intervalDurationSums[ i ],
											periodCount ) ).append( "\t" )
							.append( interval.format( maxIntervalDurations[ i ], 1 ) );
					Log.i( LOGTAG, buff.toString() );
					buff.delete( 0, buff.length() );
				}

				for( int i = 0; i < intervalDurationSums.length; i++ )
				{
					intervalDurationSums[ i ] = 0;
					intervalNames[ i ] = null;
					minIntervalDurations[ i ] = Long.MAX_VALUE;
					maxIntervalDurations[ i ] = -1;
				}
				periodDurationSum = 0;
				minPeriodDuration = Long.MAX_VALUE;
				maxPerdiodDuration = -1;
				periodCount = 0;

				lastPrintOutNanos = clickTime;
			}
		}
	}

	/**
	 * Calibrates the timer for the machine
	 * 
	 * @param numTests
	 *           10000 might be about right
	 */
	public static void recalibrate( final int numTests )
	{
		final boolean print = false;
		final CodeTimer codeTimer = new CodeTimer( "calibrate", null, null );
		// warm the JIT
		for( int i = 0; i < 1024; i++ )
		{
			codeTimer.tick( "foo" );
			codeTimer.lastTick( false );
		}

		// find how out long it takes to call click(), so that time can
		// be accounted for
		final ArrayList<Long> selfTimeObservations =
				new ArrayList<Long>( numTests );
		for( int i = 0; i < numTests; i++ )
		{
			final long nanoSelfTime = -( System.nanoTime() - System.nanoTime() );

			codeTimer.tick( "foo" );

			final long t0 = System.nanoTime();
			codeTimer.tick( "bar" );
			final long t1 = System.nanoTime();

			codeTimer.tick( "baz" );
			codeTimer.lastTick( false );

			final long currentSelfTime = t1 - t0 - nanoSelfTime;
			if( print )
			{
				Log.i( LOGTAG, "calibrating : currentSelfTime == "
						+ currentSelfTime + ", nanoSelfTime == " + nanoSelfTime );
			}
			selfTimeObservations.add( new Long( currentSelfTime ) );
		}
		// sort the times
		Collections.sort( selfTimeObservations );
		if( print )
		{
			for( int i = 0; i < selfTimeObservations.size(); i++ )
			{
				Log.i( LOGTAG, "calibrating : selfTimeObservations.get(i) == "
						+ selfTimeObservations.get( i ) );
			}
		}
		// cut out the slowest 5% which are assumed to be outliers
		for( int i = 0; i < ( int ) ( numTests * 0.05 ); i++ )
		{
			selfTimeObservations.remove( 0 );
		}
		// cut out the fastest 5% which are assumed to be outliers
		for( int i = 0; i < ( int ) ( numTests * 0.05 ); i++ )
		{
			selfTimeObservations.remove( selfTimeObservations.size() - 1 );
		}
		if( print )
		{
			Log.i( LOGTAG,
					"calibrating : Slimmed list: selfTimeObservations.size() == "
							+ selfTimeObservations.size() );
			for( int i = 0; i < selfTimeObservations.size(); i++ )
			{
				Log.i( LOGTAG, "calibrating : selfTimeObservations.get(i) == "
						+ selfTimeObservations.get( i ) );
			}
		}
		// find the average
		long sumOfSelfTimes = 0;
		for( int i = 0; i < selfTimeObservations.size(); i++ )
		{
			sumOfSelfTimes += selfTimeObservations.get( i ).longValue();
		}
		SELF_TIME = sumOfSelfTimes / selfTimeObservations.size();
		if( print )
		{
			Log.i( LOGTAG, "calibrating : SELF_TIME == " + SELF_TIME );
		}
	}

	/**
	 * Time unit that is printed
	 * 
	 * @author ryanm
	 */
	public enum Output
	{
		/**
		 * Second-level granularity
		 */
		Seconds {
			@Override
			public String format( final long totalNanos, final long count )
			{
				final double avTotalSeconds =
						( double ) totalNanos / ( count * NANOS_IN_A_SECOND );
				return fourDP.format( avTotalSeconds ) + "s";
			}
		},
		/**
		 * Millisecond-level granularity
		 */
		Millis {
			@Override
			public String format( final long totalNanos, final long count )
			{
				final double avTotalMillis =
						( double ) totalNanos / ( count * NANOS_IN_A_MILLISECOND );
				return fourDP.format( avTotalMillis ) + "ms";
			}
		},
		/**
		 * Nanosecond-level granularity
		 */
		Nanos {
			@Override
			public String format( final long totalNanos, final long count )
			{
				final double avTotalNanos = ( double ) totalNanos / count;
				return fourDP.format( avTotalNanos ) + "ns";
			}
		};

		/**
		 * @param totalNanos
		 *           The sum of some number of measurements
		 * @param count
		 *           The number of measurements
		 * @return A string describing the average time
		 */
		public abstract String format( long totalNanos, long count );
	}
}
