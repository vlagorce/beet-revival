/* 
 * Copyright 2008-2009 the original author or authors.
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 */
 
package com.mtgi.analytics;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <i>Applications should generally not have to interact with BehaviorEvent directly.</i>
 * 
 * Represents an action taken directly or indirectly by a user.
 * Each action is given a {@link #getType()}, {@link #getName()},
 * and {@link #getApplication() application};
 * has a {@link #getStart() start time} and {@link #getDuration()};
 * and is associated with a {@link #getUserId() user} and {@link #getSessionId() session id}
 * representing the user's current authenticated session with the application.
 * Other data like parameter values can be stored in a semi-structured fashion
 * in {@link #getData()}.  Generally the structure of the event data and
 * the meaning of the {@link #getName()} should depend on the {@link #getType() type}.
 * BehaviorEvents may be composed of smaller child events, which themselves
 * may be composed of child events, and so on; an event's children are
 * accessed with {@link #getChildren()}.
 * 
 * @see BehaviorEventManager
 */
public class BehaviorEvent implements Serializable {

	private static final long serialVersionUID = -5341143588240860983L;
	private static final Log log = LogFactory.getLog(BehaviorEvent.class);

	private Serializable id;
	private BehaviorEvent parent;
	private String type;
	private String name;
	private String application;
	private String userId;
	private String sessionId;
	private Date start;
	private Long startNs;
	private Long duration;
	private EventDataElement data = DeferredDataElement.INSTANCE;
	private String error;
	
	protected BehaviorEvent(BehaviorEvent parent, String type, String name, String application, String userId, String sessionId) {
		this.parent = parent;
		this.type = type;
		this.name = name;
		this.application = application;
		this.userId = userId;
		this.sessionId = sessionId;
	}

	@Override
	protected void finalize() {
		if (isStarted() && !isEnded())
			log.warn("Event [" + type + ":" + name + ":" + id + "] was started and then discarded!");
	}
	
	/**
	 * Notification that this event has started.  {@link #getStart()}
	 * will return the time at which the event began.
	 * 
	 * <i>This method should never be called directly by application code.</i>
	 * @see BehaviorTrackingManager#start(BehaviorEvent)
	 * @throws IllegalStateException if this event is already finished or started
	 */
	protected void start() {
		if (isStarted())
			throw new IllegalStateException("Event has already started");
		start = new Date();
		startNs = System.nanoTime();
	}
	
	/**
	 * Notification that this event is finished.  {@link #getDuration()}
	 * will return the time elapsed since {@link #start()} was called.
	 * 
	 * <i>This method should never be called directly by application code.</i>
	 * @see BehaviorTrackingManager#stop(BehaviorEvent)
	 * @throws IllegalStateException if this event is already finished or was never started
	 */
	protected void stop() {
		if (!isStarted())
			throw new IllegalStateException("Event has not started");
		if (isEnded())
			throw new IllegalStateException("Event has already ended");
		duration = System.nanoTime() - startNs;
	}
	
	/** @return true if this event has started already */
	protected boolean isStarted() {
		return start != null;
	}

	/** @return true if this event is finished */
	protected boolean isEnded() {
		return duration != null;
	}
	
	/** @return true if this event is not nested in some other event */
	protected boolean isRoot() {
		return parent == null;
	}
	
	/**
	 * Get the name of the application in which this event took place.
	 */
	public String getApplication() {
		return application;
	}

	/**
	 * If this event has extra data, it can be accessed here.  Otherwise returns
	 * null.
	 * @see #addData()
	 */
	public EventDataElement getData() {
		return data;
	}

	/**
	 * If this event is finished, return its duration (in milliseconds).  Otherwise return null.
	 * @deprecated to be removed after 1.4.0, use {@link #getDurationNs()} instead
	 */
	public Long getDuration() {
		return duration == null ? null : duration / 1000000;
	}
	
	/**
	 * If this event is finished, return its duration (in nanoseconds).  Otherwise return null.
	 */
	public Long getDurationNs() {
		return duration;
	}

	/** If this event ended in error, return a description of that error (null otherwise) */
	public String getError() {
		return error;
	}
	public void setError(Throwable t) {
		setError(t.getClass().getName() + ": " + t.getMessage());
	}
	
	public void setError(String error) {
		this.error = error;
	}
	
	/** a unique identifier (e.g. primary key) for this event */
	public Serializable getId() {
		return id;
	}
	public void setId(Serializable id) {
		this.id = id;
	}
	
	/**
	 * Get the name of the event; for example, a method name for
	 * instrumented method calls, or a server request path for
	 * an instrumented servlet.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * If this event is a child of a larger composite event, 
	 * return the event's parent.  Otherwise returns null.
	 */
	public BehaviorEvent getParent() {
		return parent;
	}
	
	/**
	 * If this event is part of an authenticated session, return an
	 * identifier of that session.
	 */
	public String getSessionId() {
		return sessionId;
	}
	
	/**
	 * If this event has started, return the date at which the event began.
	 * Otherwise return null.
	 */
	public Date getStart() {
		return start;
	}

	/**
	 * Get the type of this event.  Many applications will only use
	 * one type of event (e.g. "method"), though some may define several.
	 * A single application should generally have only a smaller number of
	 * event types, corresponding to the layer of the system at which the
	 * event originated (e.g. "method", "rendering", "database").  It is
	 * suggested that the value of "type" should give some meaning to how
	 * the value of {@link #getName()} should be interpreted.  For example,
	 * an event type of "request" could signify that {@link #getName()} returns
	 * an HTTP request URL.  Generally though type/name schemes are up to the application.
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Get the user on whose behalf this event is executing.
	 */
	public String getUserId() {
		return userId;
	}
	
	/**
	 * Add metadata to this event, if metadata has not already been added.
	 * @return A container for event data.  Calling this method more than once always returns the same instance.
	 */
	public EventDataElement addData() {
		return data.initialize(this);
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("behavior-event:").append(" id=\"").append(id).append('"');
		if (parent != null)
			buf.append(" parent-id=\"").append(parent.getId()).append('"');
		buf.append(" type=\"").append(type).append('"')
		   .append(" name=\"").append(name).append('"')
		   .append(" application=\"").append(application).append('"')
		   .append(" start=\"").append(start).append('"')
		   .append(" duration-ns=\"").append(duration).append('"')
		   .append(" user-id=\"").append(userId).append('"')
		   .append(" session-id=\"").append(sessionId).append('"')
		   .append(" error=\"").append(error).append('"');
		
		return buf.toString();
	}
	
	/** 
	 * a singleton empty data element, for events that do not need to define any data.
	 * Replaces itself with a new, standard EventDataElement instance on the first call
	 * to {@link #initialize(BehaviorEvent)}.  This somewhat arcane construction is an optimization
	 * for calls to {@link #addData}, so that they do not require a null check.
	 */
	private static class DeferredDataElement extends ImmutableEventDataElement {
		private static final long serialVersionUID = -4571142241188203441L;
		private static final DeferredDataElement INSTANCE = new DeferredDataElement();
		private DeferredDataElement() {
			super("event-data");
		}
		
		@Override
		public boolean isNull() {
			return true;
		}

		@Override
		protected EventDataElement initialize(BehaviorEvent event) {
			return event.data = new EventDataElement("event-data");
		}
	}
}
