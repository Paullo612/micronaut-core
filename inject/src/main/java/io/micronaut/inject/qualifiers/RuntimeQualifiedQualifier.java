/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.inject.qualifiers;

import io.micronaut.context.Qualifier;
import io.micronaut.context.annotation.RuntimeQualified;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.BeanType;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * A Qualifier wrapper that creates another Qualifier using factory specified by
 * {@link io.micronaut.context.annotation.RuntimeQualified} annotation at its construction time.
 *
 * @param <T> The qualifier type
 *
 * @author Paullo612
 * @since 3.4.3
 */
class RuntimeQualifiedQualifier<T> extends AbstractRuntimeQualifiedQualifier<T> {

    private final Qualifier<T> delegate;

    RuntimeQualifiedQualifier(Qualifier<T> delegate) {
        this.delegate = delegate;
    }

    RuntimeQualifiedQualifier(AnnotationMetadata metadata) {
        AnnotationValue<RuntimeQualified> annotationValue = getAnnotationValue(metadata);
        AnnotationClassValue<?> annotationClassValue = getAnnotationClassValue(annotationValue);
        QualifierFactory qualifierFactory = getQualifierFactory(annotationClassValue);

        this.delegate = qualifierFactory.createQualifier(metadata);
    }

    @Override
    public <BT extends BeanType<T>> Stream<BT> reduce(Class<T> beanType, Stream<BT> candidates) {
        return delegate.reduce(beanType, candidates);
    }

    @Override
    public boolean contains(Qualifier<T> qualifier) {
        return super.contains(qualifier) || delegate.contains(qualifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RuntimeQualifiedQualifier<?> that = (RuntimeQualifiedQualifier<?>) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return "@RuntimeQualified(" + delegate + ")";
    }
}
