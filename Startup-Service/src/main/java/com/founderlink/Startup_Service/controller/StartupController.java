package com.founderlink.Startup_Service.controller;

import com.founderlink.Startup_Service.dto.StartupRequestDto;
import com.founderlink.Startup_Service.dto.StartupResponseDto;
import com.founderlink.Startup_Service.service.StartupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/startup")
@RequiredArgsConstructor
public class StartupController {

    private final StartupService service;

    @PostMapping
    public ResponseEntity<StartupResponseDto> create(
            @Valid @RequestBody StartupRequestDto dto
    ) {
        return ResponseEntity.ok(service.createStartup(dto));
    }

    @GetMapping
    public ResponseEntity<List<StartupResponseDto>> search(
            @RequestParam String industry,
            @RequestParam String stage,
            @RequestParam Double fundingGoal
    ){
        return ResponseEntity.ok(service.searchStartup(industry, stage, fundingGoal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StartupResponseDto> get(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StartupResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody StartupRequestDto dto
    ){
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id
    ){
        service.delete(id);
        return ResponseEntity.ok("Deleted Successfully");
    }
}
