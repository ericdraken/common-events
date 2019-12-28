/*
 * Copyright (c) 2019. Eric Draken - ericdraken.com
 */

package com.ericdraken.common.events;

import javax.annotation.Nonnegative;

public interface EventSubscriberMethods
{
	default void serviceTrouble() {}

	default void networkTrouble() {}

	default void systemTrouble() {}

	default void updateApiRate( @Nonnegative double rate ) {}

	default void uploadEvent() {}

	default void dbOperationEvent( boolean res ) {}

	default void heartbeat() {}

	default void resetState() {}
}
