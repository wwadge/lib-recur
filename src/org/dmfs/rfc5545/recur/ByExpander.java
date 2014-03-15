/*
 * Copyright (C) 2013 Marten Gajda <marten@dmfs.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.dmfs.rfc5545.recur;

/**
 * An abstract by-part filter. Depending on a parameter it expands or limits the instances returned by the previous filter.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
abstract class ByExpander extends RuleIterator
{

	/**
	 * Stop iterating (throwing an exception) if this number of empty sets passed in a line, i.e. sets that contain no elements because they have been filtered
	 * or nothing was expanded.
	 */
	private final static int MAX_EMPTY_SETS = 1000;

	/**
	 * The scope of a part. Depending on the frequency and preceding filters some filters operate within a specific scope.
	 */
	protected enum Scope
	{
		WEEKLY, MONTHLY, WEEKLY_AND_MONTHLY, YEARLY;
	}

	/**
	 * The first instance to iterate.
	 */
	private final long mStart;

	/**
	 * The set we work on.
	 */
	private LongArray mWorkingSet = null;

	/**
	 * The set we return.
	 */
	private final LongArray mResultSet = new LongArray();

	/**
	 * The {@link CalendarMetrics} to use.
	 */
	final CalendarMetrics mCalendarMetrics;

	/**
	 * The filters to apply after an expansion. We never have more than 8 filters.
	 */
	private final ByFilter[] mFilters = new ByFilter[8];

	/**
	 * The number of {@link ByFilter}s in {@link #mFilters}.
	 */
	int mFilterCount = 0;

	/**
	 * Indicates that we have to sort the result.
	 */
	private boolean mNeedsSorting = false;


	/**
	 * Create a new filter that filters the instances returned by the previous {@link RuleIterator}. The parameter <code>expand</code> determines whether the
	 * filter should limit or expand the instances.
	 * 
	 * @param previous
	 *            The preceding {@link RuleIterator}.
	 * @param start
	 *            The first instance.
	 */
	public ByExpander(RuleIterator previous, CalendarMetrics calendarTools, long start)
	{
		super(previous);
		mStart = start;
		mCalendarMetrics = calendarTools;
	}


	/**
	 * Set whether the resulting set needs sorting.
	 * 
	 * @param needsSorting
	 *            <code>true</code> if the the set needs to be sorted, <code>false</code>otherwise.
	 */
	void setNeedsSorting(boolean needsSorting)
	{
		mNeedsSorting = needsSorting;
	}


	@Override
	public long next()
	{
		long next;
		if (mWorkingSet == null || !mWorkingSet.hasNext())
		{
			mWorkingSet = nextSet();
		}
		next = mWorkingSet.next();
		return next;
	}


	@Override
	LongArray nextSet()
	{
		LongArray resultSet = mResultSet;
		resultSet.clear();

		int counter = 0;
		do
		{
			if (counter == MAX_EMPTY_SETS)
			{
				throw new IllegalArgumentException("too many empty recurrence sets " + this);
			}
			counter++;

			LongArray prev = mPrevious.nextSet();
			while (prev.hasNext())
			{
				expand(prev.next(), mStart);
			}
		} while (!resultSet.hasNext());

		if (mNeedsSorting)
		{
			resultSet.sort();
		}

		return resultSet;
	}


	/**
	 * Add a filter to this expander. A filter is applied before an instance is added to the resulting set.
	 * 
	 * @param filter
	 *            The {@link ByFilter} to add.
	 */
	final void addFilter(ByFilter filter)
	{
		mFilters[mFilterCount++] = filter;
	}


	/**
	 * Add an instance to the result set. The instance is not added if it's filtered.
	 * 
	 * @param instance
	 *            The instance to add.
	 */
	final void addInstance(long instance)
	{
		if (mFilterCount == 0 || !filter(instance))
		{
			mResultSet.add(instance);
		}
	}


	/**
	 * Filter an instance. This method determines if a given {@link Instance} should be removed from the result set or not.
	 * 
	 * @param instance
	 *            The instance to filter.
	 * @return <code>true</code> to remove the instance from the result set, <code>false</code> to include it.
	 */
	final boolean filter(long instance)
	{
		ByFilter[] filters = mFilters;
		for (int i = 0, count = mFilterCount; i < count; ++i)
		{
			if (filters[i].filter(instance))
			{
				return true;
			}
		}
		return false;
	}


	/**
	 * Expand an instance. This method expands an {@link Instance}.
	 * 
	 * @param instance
	 *            The instance to expand.
	 * @param start
	 *            The first instance of the rule. An implementing filter can use this to avoid iterating instances that precede the first instance if it's save
	 *            to do so.
	 */
	abstract void expand(long instance, long start);
}