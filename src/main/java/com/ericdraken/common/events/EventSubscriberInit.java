/*
 * Copyright (c) 2019. Eric Draken - ericdraken.com
 */

package com.ericdraken.common.events;

public interface EventSubscriberInit
{
	void init() throws Exception;

	void shutdown() throws Exception;

	String getDescription();

	boolean isReady();
}
