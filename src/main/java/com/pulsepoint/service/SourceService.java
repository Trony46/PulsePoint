package com.pulsepoint.service;

import com.pulsepoint.model.Source;
import com.pulsepoint.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceService {
    private final SourceRepository sourceRepository;

    public Source createSource(Source source){
        source.setActive(true);
        source.setRegisteredAt(LocalDateTime.now());
        source.setLastSeenAt(null);
        return sourceRepository.save(source);
    }
    public List<Source> getAllSources(){
        return sourceRepository.findAll();
    }

    public Source getSourceById(Long id){
        return sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Source not found with id: " + id));
    }
    //
    public Source findSourceOrThrow(Long id) {
        return getSourceById(id);
    }
    //
}
