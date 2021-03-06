/*
 * Copyright 2018 The OpenTracing Authors
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
 */
package io.opentracing.contrib.jfrtracer.jfr;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.StackTrace;

import javax.management.DescriptorKey;

import io.opentracing.Span;
import io.opentracing.contrib.jfrtracer.ContextExtractor;
import io.opentracing.contrib.jfrtracer.jfr.AbstractJfrSpanEmitterImpl;

/**
 * This is the JDK 9 or later implementation of the JfrEmitter.
 */
public class JfrSpanEmitterImpl extends AbstractJfrSpanEmitterImpl {
	private Jdk9SpanEvent currentEvent;

	@Label("Span Event")
	@Description("Open tracing event corresponding to a span.")
	@Category("Open Tracing")
	@StackTrace(false)
	private static class Jdk9SpanEvent extends Event {
		@Label("Operation Name")
		@Description("The operation name for the span")
		private String operationName;
		
		@Label("Trace Id")
		@Description("The trace id for the span")
		private String traceId;

		@Label("Span Id")
		@Description("The id of the parent span")
		private String spanId;

		@Label("Parent Id")
		@Description("The id of the parent span")
		private String parentId;
		
		@Label("Start Thread")
		@Description("The thread initiating the span")
		private Thread startThread;
		
		@Label("End Thread")
		@Description("The thread ending the span")
		private Thread endThread;
	}
	
	private static class EndEventCommand implements Runnable {
		private final Jdk9SpanEvent event;

		public EndEventCommand(Jdk9SpanEvent event) {
			this.event = event;
		}

		@Override
		public void run() {
			event.commit();
		}
	}

	private static class BeginEventCommand implements Runnable {
		private final Jdk9SpanEvent event;

		public BeginEventCommand(Jdk9SpanEvent event) {
			this.event = event;
		}

		@Override
		public void run() {
			event.begin();
		}
	}

	JfrSpanEmitterImpl(Span span, ContextExtractor extractor) {
		super(span, extractor);
	}

	@Override
	public void close() throws Exception {
		if (currentEvent != null) {
			currentEvent.endThread = Thread.currentThread();
			EXECUTOR.execute(new EndEventCommand(currentEvent));
			currentEvent = null;
		} else {
			LOGGER.warning("Close without start discovered!");
		}
	}

	@Override
	public void start() {
		currentEvent = new Jdk9SpanEvent();
		if (extractor != null) {
			currentEvent.operationName = extractor.extractOperationName(span);
			currentEvent.traceId = extractor.extractTraceId(span);
			currentEvent.spanId = extractor.extractSpanId(span);
			currentEvent.parentId = extractor.extractParentId(span);
			currentEvent.startThread = Thread.currentThread();
		} else {
			LOGGER.warning(
					"Trying to create event when no valid extractor is available. Create an extractor for your particular open tracing tracer implementation, and register it with the ExtractorRegistry.");
		}
		EXECUTOR.execute(new BeginEventCommand(currentEvent));
	}

	@Override
	public String toString() {
		return "JDK 9+ JFR Emitter for " + extractor.getSupportedTracerType() + "/" + extractor.getSupportedSpanType();
	}
}
