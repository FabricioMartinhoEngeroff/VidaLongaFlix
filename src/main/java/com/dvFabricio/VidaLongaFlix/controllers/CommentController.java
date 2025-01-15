//package com.dvFabricio.VidaLongaFlix.controllers;
//
//
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentDTO;
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.ErrorResponse;
//import com.dvFabricio.VidaLongaFlix.infra.exception.MissingRequiredFieldException;
//import com.dvFabricio.VidaLongaFlix.services.CommentService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/comments")
//public class CommentController {
//
//    private final CommentService commentService;
//
//    public CommentController(CommentService commentService) {
//        this.commentService = commentService;
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<?> getCommentById(@PathVariable UUID id) {
//        try {
//            CommentDTO commentDTO = commentService.getCommentDTOById(id);
//            return ResponseEntity.ok(commentDTO);
//        } catch (MissingRequiredFieldException e) {
//            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
//        }
//    }
//}
