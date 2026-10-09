package com.poke.shared.mapping;

import org.mapstruct.MapperConfig;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.mapstruct.ReportingPolicy.ERROR;

@MapperConfig(componentModel = SPRING, injectionStrategy = CONSTRUCTOR, unmappedTargetPolicy = ERROR)
public interface MappingConfig {
}
