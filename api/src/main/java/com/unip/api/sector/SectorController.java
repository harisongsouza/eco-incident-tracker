package com.unip.api.sector;

import com.unip.api.sector.dto.SectorResponse;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/sectors")
public class SectorController {

    private final SectorRepository sectors;

    public SectorController(SectorRepository sectors) {
        this.sectors = sectors;
    }

    @GetMapping
    public List<SectorResponse> list() {
        return sectors.findAll(Sort.by("name")).stream()
                .map(s -> new SectorResponse(s.getId(), s.getName()))
                .toList();
    }
}