package com.dvFabricio.VidaLongaFlix.services;


import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentDTO;
import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import com.dvFabricio.VidaLongaFlix.infra.exception.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.repositories.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;


    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public CommentDTO getCommentDTOById(UUID commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new MissingRequiredFieldException("comment", "Comentário não encontrado."));
        validateComment(comment);
        return new CommentDTO(comment);
    }

    private void validateComment(Comment comment) {
        if (comment.getUser() == null) {
            throw new MissingRequiredFieldException("user", "Um comentário deve estar associado a um usuário válido.");
        }
        if (comment.getVideo() == null) {
            throw new MissingRequiredFieldException("video", "Um comentário deve estar associado a um vídeo válido.");
        }
        if (comment.getText() == null || comment.getText().isBlank()) {
            throw new MissingRequiredFieldException("text", "O texto do comentário não pode estar vazio.");
        }
    }
}

