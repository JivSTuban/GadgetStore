package com.tuban.GadgetStore.Controllers;

import com.tuban.GadgetStore.Entities.ProductsEntity;
import com.tuban.GadgetStore.Models.ProductsModel;
import com.tuban.GadgetStore.Services.ProductsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@Controller
@RequestMapping("/products")
public class ProductsController {

    @Autowired
    private ProductsRepository productsRepository;

    // I don't want to reveal the images directory to the clients, so I hid it
    @Value("${app.images.directory}")
    private String imagesDirectory;

    @Value("${app.images.uploadDirectory}")
    private String uploadDirectory;

    @GetMapping({"","/"})
    public String showProductsList(Model model){
        List<ProductsEntity> productsEntities = productsRepository.findAll();

        /*
            I'm adding the images directory attribute to the products page,
            so I can use it to render images from that directory into the page
        */
        for (ProductsEntity productsEntity : productsEntities){
            productsEntity.setImageFileName(imagesDirectory+ productsEntity.getImageFileName());
        }
        model.addAttribute("productsEntities", productsEntities);
        return "products/index";
    }


    @GetMapping("/create")
    public String showCreatePage(Model model){
        ProductsModel productsModel = new ProductsModel();
        model.addAttribute("productsModel", productsModel);
        return "products/createProduct";
    }

    @PostMapping("/create")
    public String createProduct(
            @Valid @ModelAttribute ProductsModel productsModel,
            BindingResult result
            ){
        if (productsModel.getImageFile().isEmpty()){
            result.addError(new FieldError("productsModel", "imageFile", "The image file is required."));
        }
        if (result.hasErrors()){
            return "products/createProduct";
        }

        // Save the image to the server after checking for errors
        MultipartFile image = productsModel.getImageFile();
        Date createdAt = new Date();
        String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

        try {
            Path uploadPath = Paths.get(uploadDirectory);

            if(!Files.exists(uploadPath)){
                Files.createDirectories(uploadPath);
            }

            try(InputStream inputStream = image.getInputStream()){
                Files.copy(inputStream, Paths.get(uploadDirectory+storageFileName),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception e) {
            System.out.println("Exception: "+e.getMessage());
        }

        // Create a new product and transfer the newly created product model to this object
        ProductsEntity productsEntity = new ProductsEntity();
        productsEntity.setName(productsModel.getName());
        productsEntity.setBrand(productsModel.getBrand());
        productsEntity.setCategory(productsModel.getCategory());
        productsEntity.setPrice(productsModel.getPrice());
        productsEntity.setDescription(productsModel.getDescription());
        productsEntity.setCreatedAt(createdAt);
        productsEntity.setImageFileName(storageFileName);

        // Save the new product to the database
        productsRepository.save(productsEntity);

        return "redirect:/products";
    }

    @GetMapping("/edit")
    public String showEditPage(Model model,
                               @RequestParam int id){
        try{
            ProductsEntity productsEntity = productsRepository.findById(id).get();
            productsEntity.setImageFileName(imagesDirectory+ productsEntity.getImageFileName());
            model.addAttribute("productsEntity", productsEntity);

            ProductsModel productsModel = new ProductsModel();
            productsModel.setName(productsEntity.getName());
            productsModel.setBrand(productsEntity.getBrand());
            productsModel.setCategory(productsEntity.getCategory());
            productsModel.setPrice(productsEntity.getPrice());
            productsModel.setDescription(productsEntity.getDescription());



            model.addAttribute("productsModel", productsModel);
        }catch(Exception e){
            System.out.println("Exception: "+e.getMessage());
            return "redirect:/products";
        }
        return "products/editProduct";
    }

    @PostMapping("/edit")
    public String updateProduct(Model model,
                                @RequestParam int id,
                                @Valid @ModelAttribute ProductsModel productsModel,
                                BindingResult result){

        try{
            ProductsEntity productsEntity = productsRepository.findById(id).get();
            productsEntity.setImageFileName(imagesDirectory+ productsEntity.getImageFileName());
            model.addAttribute("product", productsEntity);

            if (result.hasErrors()){
                return "products/editProduct";
            }

            if (!productsModel.getImageFile().isEmpty()){
                // Delete old image stored in the server
                Path oldImagePath = Paths.get(uploadDirectory+ productsEntity.getImageFileName());
                try{
                    Files.delete(oldImagePath);
                }catch(Exception e){
                    System.out.println("Exception: " + e.getMessage());
                }

                // Save the image to the server after checking for errors
                MultipartFile image = productsModel.getImageFile();
                Date createdAt = new Date();
                String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

                try(InputStream inputStream = image.getInputStream()){
                    Files.copy(inputStream, Paths.get(uploadDirectory+storageFileName),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                productsEntity.setImageFileName(storageFileName);
            }
            productsEntity.setName(productsModel.getName());
            productsEntity.setBrand(productsModel.getBrand());
            productsEntity.setCategory(productsModel.getCategory());
            productsEntity.setPrice(productsModel.getPrice());
            productsEntity.setDescription(productsModel.getDescription());

            productsRepository.save(productsEntity);
        }catch(Exception e){
            System.out.println("Exception: "+e.getMessage());
            return "redirect:/products";
        }

        return "redirect:/products";
    }

    @GetMapping("/delete")
    public String deleteProduct(@RequestParam int id){
        try {
            ProductsEntity productsEntity = productsRepository.findById(id).get();

            // Delete image stored in the server
            Path imagePath = Paths.get(uploadDirectory+ productsEntity.getImageFileName());
            try{
                Files.delete(imagePath);
            }catch(Exception e){
                System.out.println("Exception: " + e.getMessage());
            }

            productsRepository.delete(productsEntity);
        } catch (Exception e) {
            System.out.println("Exception: "+e.getMessage());
        }
        return "redirect:/products";
    }
}
