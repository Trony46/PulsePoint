package com.pulsepoint.controller;

import com.pulsepoint.model.Source;
import com.pulsepoint.service.SourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SourceController {
    private final SourceService sourceService;

    @PostMapping
    public ResponseEntity<Source> createSource(@Valid @RequestBody Source s){
        return new ResponseEntity<>(sourceService.createSource(s), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Source>> getAllSources(){
        return new ResponseEntity<>(sourceService.getAllSources(),HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Source> getSource(@PathVariable Long id){
        return new ResponseEntity<>(sourceService.getSourceById(id),HttpStatus.OK);
    }
}
