package com.nyayaai.backend.search;

import com.nyayaai.backend.embedding.OllamaEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalSearchServiceTest {

    @Mock
    private OllamaEmbeddingClient embeddingClient;

    @Mock
    private LegalSearchRepository searchRepository;

    @Test
    void rejectsUnexpectedEmbeddingDimension() {
        when(embeddingClient.embed("contract termination"))
                .thenReturn(List.of(1.0, 2.0));

        LegalSearchService service =
                new LegalSearchService(embeddingClient, searchRepository);

        assertThrows(
                IllegalStateException.class,
                () -> service.search("contract termination", 5)
        );
        verify(searchRepository, never()).findNearest("[1.0, 2.0]", 5);
    }
}