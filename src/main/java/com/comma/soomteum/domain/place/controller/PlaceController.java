package com.comma.soomteum.domain.place.controller;

import com.comma.soomteum.domain.ai.adapter.AiServiceAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "여행지", description = "여행지와 관련된 로직")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {


}
