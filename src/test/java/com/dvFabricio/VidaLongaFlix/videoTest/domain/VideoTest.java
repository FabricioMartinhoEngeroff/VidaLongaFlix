//package com.dvFabricio.VidaLongaFlix.videoTest.domain;
//
//
//import com.dvFabricio.VidaLongaFlix.domain.category.Category;
//import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
//import com.dvFabricio.VidaLongaFlix.domain.video.Video;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class VideoTest {
//
//    @Test
//    void testVideoCreation() {
//        Category category = new Category("Education");
//        Video video = Video.builder().title("Introduction to Java").description("A beginner's guide to Java programming.").url("http://example.com/java-intro").category(category).views(100).watchTime(15.5).build();
//
//        assertNotNull(video);
//        assertEquals("Introduction to Java", video.getTitle());
//        assertEquals("A beginner's guide to Java programming.", video.getDescription());
//        assertEquals("http://example.com/java-intro", video.getUrl());
//        assertEquals(category, video.getCategory());
//        assertEquals(100, video.getViews());
//        assertEquals(15.5, video.getWatchTime());
//    }
//
//    @Test
//    void testVideoCommentsAssociation() {
//        Category category = new Category("Education");
//        Video video = Video.builder().title("Advanced Java").description("Learn advanced Java techniques.").url("http://example.com/java-advanced").category(category).build();
//
//        Comment comment1 = new Comment();
//        comment1.setText("Great tutorial!");
//        Comment comment2 = new Comment();
//        comment2.setText("Very helpful, thanks!");
//
//        video.setComments(List.of(comment1, comment2));
//
//        assertNotNull(video.getComments());
//        assertEquals(2, video.getComments().size());
//        assertTrue(video.getComments().stream().anyMatch(comment -> comment.getText().equals("Great tutorial!")));
//        assertTrue(video.getComments().stream().anyMatch(comment -> comment.getText().equals("Very helpful, thanks!")));
//    }
//
//    @Test
//    void testVideoDefaultValues() {
//        Category category = new Category("Entertainment");
//        Video video = Video.builder().title("Funny Cats Compilation").description("A compilation of funny cat videos.").url("http://example.com/funny-cats").category(category).build();
//
//        assertEquals(0, video.getViews());
//        assertEquals(0.0, video.getWatchTime());
//        assertTrue(video.getComments().isEmpty());
//    }
//
//    @Test
//    void testVideoSetters() {
//        Video video = new Video();
//        Category category = new Category("Lifestyle");
//
//        video.setTitle("Daily Yoga Routine");
//        video.setDescription("A step-by-step guide to daily yoga.");
//        video.setUrl("http://example.com/yoga-routine");
//        video.setCategory(category);
//        video.setViews(500);
//        video.setWatchTime(45.0);
//
//        assertEquals("Daily Yoga Routine", video.getTitle());
//        assertEquals("A step-by-step guide to daily yoga.", video.getDescription());
//        assertEquals("http://example.com/yoga-routine", video.getUrl());
//        assertEquals(category, video.getCategory());
//        assertEquals(500, video.getViews());
//        assertEquals(45.0, video.getWatchTime());
//    }
//}
