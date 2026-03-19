package com.founderlink.Startup_Service.service;

import com.founderlink.Startup_Service.dto.StartupRequestDto;
import com.founderlink.Startup_Service.dto.StartupResponseDto;
import com.founderlink.Startup_Service.entity.Startup;
import com.founderlink.Startup_Service.event.StartupCreatedEvent;
import com.founderlink.Startup_Service.repository.StartupRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StartupService {

    private final StartupRepository repository;
    private final ModelMapper modelMapper;
    private final RabbitTemplate rabbitTemplate;


    public StartupResponseDto createStartup(StartupRequestDto dto) {
        Startup startup = modelMapper.map(dto, Startup.class);
        startup.setStage(Startup.Stage.valueOf(dto.getStage()));

        Startup saved = repository.save(startup);

        rabbitTemplate.convertAndSend(
                "startup-exchange",
                "startup.created",
                new StartupCreatedEvent(
                        saved.getId(),
                        saved.getFounderId(),
                        saved.getIndustry(),
                        saved.getFundingGoal()
                )
        );

        return modelMapper.map(saved, StartupResponseDto.class);
    }

    public List<StartupResponseDto> searchStartup(String industry, String stage, Double fundingGoal) {
        return repository
                .findByIndustryContainingAndStageAndFundingGoalLessThanEqual(
                        industry,
                        Startup.Stage.valueOf(stage),
                        fundingGoal
                )
                .stream()
                .map(s -> modelMapper.map(s, StartupResponseDto.class))
                .toList();
    }

    public StartupResponseDto getById(Long id) {
        Startup s = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        return modelMapper.map(s, StartupResponseDto.class);
    }

    public StartupResponseDto update(Long id, StartupRequestDto dto) {
        Startup s = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not Found"));

        modelMapper.map(dto, s);
        s.setStage(Startup.Stage.valueOf(dto.getStage()));

        return modelMapper.map(repository.save(s), StartupResponseDto.class);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }


}
