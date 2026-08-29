package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.brand.BrandRequest;
import com.example.spring_boot_project_api.dto.response.brand.BrandResponse;
import com.example.spring_boot_project_api.service.BrandService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/brands")
@Validated
public class BrandController {
    private final BrandService brandService;
    public BrandController(BrandService brandService){
        this.brandService = brandService;
    }
    //Create Brand
    @PostMapping
    public ResponseEntity<BrandResponse> createBrand(
        @Valid @RequestBody BrandRequest request
    ){
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    //Get all brands
    @GetMapping
    public ResponseEntity<List<BrandResponse>>getAllBrands(){
        List<BrandResponse> response = brandService.getAllBrands();
        return ResponseEntity.ok(response);
    }

    //Get Brand By Id
    @GetMapping("/{id}")
    public ResponseEntity<BrandResponse> getBrandById(
        @PathVariable Long id){
            BrandResponse response = brandService.getBrandById(id);

            return ResponseEntity.ok(response);
        }
    //Update Brand
    @PutMapping("/{id}")
    public ResponseEntity<BrandResponse> updateBrand(
        @PathVariable Long id,
        @Valid @RequestBody BrandRequest request){
            BrandResponse response = brandService.updateBrand(id, request);

            return ResponseEntity.ok(response);
        }
    //Soft Delete Brand
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateBrand(@PathVariable Long id){
        brandService.deactivateBrand(id);

        return ResponseEntity.noContent().build();
    }
}