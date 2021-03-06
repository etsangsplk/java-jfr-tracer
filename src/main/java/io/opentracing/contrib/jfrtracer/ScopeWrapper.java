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
package io.opentracing.contrib.jfrtracer;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.contrib.jfrtracer.jfr.JfrEmitter;

/**
 * Wrapper for {@link Scope}.
 */
final class ScopeWrapper implements Scope {
	private final Scope delegate;
	private final JfrEmitter emitter;
	private final SpanWrapper spanWrapper;

	ScopeWrapper(SpanWrapper spanWrapper, Scope delegate, ContextExtractor extractor) {
		this.spanWrapper = spanWrapper;
		this.delegate = delegate;
		emitter = SpanWrapper.EMITTER_FACTORY.createScopeEmitter(delegate.span(), extractor);
		emitter.start();
	}

	@Override
	public void close() {
		delegate.close();
		try {
			emitter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Span span() {
		return spanWrapper;
	}
}
