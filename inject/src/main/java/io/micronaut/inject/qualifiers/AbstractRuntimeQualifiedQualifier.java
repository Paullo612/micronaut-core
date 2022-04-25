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
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.reflect.InstantiationUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.type.Argument;

import java.lang.reflect.Constructor;
import java.util.Optional;

/**
 * A base class for Qualifier wrappers that work with {@link io.micronaut.context.annotation.RuntimeQualified}
 * annotation.
 *
 * @param <T> The qualifier type
 *
 * @author Paullo612
 * @since 3.4.3
 */
@Internal
abstract class AbstractRuntimeQualifiedQualifier<T> implements Qualifier<T> {

    private static final String MEMBER_LAZY = "lazy";

    static <T> Qualifier<T> create(Argument<?> argument) {
        if (!argument.isProvider()) {
            return new RuntimeQualifiedQualifier<>(argument.getAnnotationMetadata());
        }

        AnnotationMetadata metadata = argument.getAnnotationMetadata();
        AnnotationValue<RuntimeQualified> annotationValue = getAnnotationValue(metadata);

        boolean isLazy = annotationValue.booleanValue(MEMBER_LAZY)
                .orElse(false);

        if (!isLazy) {
            return new RuntimeQualifiedQualifier<>(metadata);
        }

        return new LazyRuntimeQualifiedQualifier<>(metadata, annotationValue);
    }

    static AnnotationValue<RuntimeQualified> getAnnotationValue(AnnotationMetadata metadata) {
        return Optional.ofNullable(metadata.getAnnotation(RuntimeQualified.class))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No [RuntimeQualified] annotation present in annotation metadata"
                ));
    }

    static AnnotationClassValue<?> getAnnotationClassValue(AnnotationValue<RuntimeQualified> annotationValue) {
        return annotationValue.annotationClassValue(AnnotationMetadata.VALUE_MEMBER)
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to get required annotation field [" + AnnotationMetadata.VALUE_MEMBER
                                + "] from annotation metadata"
                ));
    }

    static QualifierFactory getQualifierFactory(AnnotationClassValue<?> annotationClassValue) {
        return doGetQualifierFactory(annotationClassValue)
                .orElseThrow(() -> new IllegalStateException("Failed to instantiate QualifierFactory"));
    }

    @SuppressWarnings("unchecked")
    private static Optional<? extends QualifierFactory> doGetQualifierFactory(
            AnnotationClassValue<?> annotationClassValue) {
        Object instance = annotationClassValue.getInstance().orElse(null);
        if (instance instanceof QualifierFactory) {
            return Optional.of((QualifierFactory) instance);
        }

        Class<?> qualifierFactoryClass = annotationClassValue.getType().orElse(null);

        if (qualifierFactoryClass == null || !QualifierFactory.class.isAssignableFrom(qualifierFactoryClass)) {
            return Optional.empty();
        }

        Optional<? extends QualifierFactory> qualifierFactory =
                InstantiationUtils.tryInstantiate(qualifierFactoryClass.asSubclass(QualifierFactory.class));

        if (qualifierFactory.isPresent()) {
            return qualifierFactory;
        }

        // maybe a Groovy closure
        Optional<Constructor<?>> optionalConstructor =
                ReflectionUtils.findConstructor((Class) qualifierFactoryClass, Object.class, Object.class);
        return optionalConstructor.flatMap(constructor -> InstantiationUtils.tryInstantiate(constructor, null, null))
                .flatMap(obj -> ReflectionUtils.findMethod(obj.getClass(), "call", AnnotationMetadata.class)
                        .map(method -> new QualifierFactory() {

                            @Override
                            public <Q> Qualifier<Q> createQualifier(AnnotationMetadata metadata) {
                                Object result = ReflectionUtils.invokeMethod(obj, method, metadata);
                                if (result != null && !(result instanceof Qualifier)) {
                                    throw new IllegalStateException(
                                            "QualifierFactory represented as groovy closure returned incorrect type: "
                                            + "(" + result.getClass().getSimpleName() + " vs Qualifier)"
                                    );
                                }

                                return (Qualifier<Q>) result;
                            }
                        })
                );
    }
}
