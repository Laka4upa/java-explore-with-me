package ru.practicum.model.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.model.dto.CompilationDto;
import ru.practicum.model.dto.NewCompilationDto;
import ru.practicum.model.entity.Compilation;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CompilationMapper {

    private final EventMapper eventMapper;

    public CompilationMapper(EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public Compilation toEntity(NewCompilationDto compilationDto) {
        return Compilation.builder()
                .title(compilationDto.getTitle())
                .pinned(compilationDto.getPinned() != null ? compilationDto.getPinned() : false)
                .events(new HashSet<>())
                .build();
    }

    public CompilationDto toDto(Compilation compilation) {
        if (compilation == null) return null;

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents().stream()
                        .map(event -> eventMapper.toShortDto(event, 0, 0L)) // confirmedRequests и views будут установлены в сервисе
                        .collect(Collectors.toList()))
                .build();
    }

    public List<CompilationDto> toDtoList(List<Compilation> compilations) {
        return compilations.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}