package net.catenax.irs.aaswrapper.job;

import static net.catenax.irs.util.TestMother.jobParameter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import net.catenax.irs.InMemoryBlobStore;
import net.catenax.irs.aaswrapper.job.delegate.DigitalTwinDelegate;
import net.catenax.irs.connector.job.ResponseStatus;
import net.catenax.irs.connector.job.TransferInitiateResponse;
import net.catenax.irs.util.TestMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AASTransferProcessManagerTest {

    private final TestMother generate = new TestMother();

    DigitalTwinDelegate digitalTwinProcessor = mock(DigitalTwinDelegate.class);
    ExecutorService pool = mock(ExecutorService.class);

    final AASTransferProcessManager manager = new AASTransferProcessManager(digitalTwinProcessor, pool, new InMemoryBlobStore());

    @Test
    void shouldExecuteThreadForProcessing() {
        // given
        final ItemDataRequest itemDataRequest = ItemDataRequest.rootNode(UUID.randomUUID().toString());

        // when
        manager.initiateRequest(itemDataRequest, s -> {
        }, aasTransferProcess -> {
        }, jobParameter());

        // then
        verify(pool, times(1)).execute(any(Runnable.class));
    }

    @Test
    void shouldInitiateProcessingAndReturnOkStatus() {
        // given
        final ItemDataRequest itemDataRequest = ItemDataRequest.rootNode(UUID.randomUUID().toString());

        // when
        final TransferInitiateResponse initiateResponse = manager.initiateRequest(itemDataRequest, s -> {
        }, aasTransferProcess -> {
        }, jobParameter());

        // then
        assertThat(initiateResponse.getTransferId()).isNotBlank();
        assertThat(initiateResponse.getStatus()).isEqualTo(ResponseStatus.OK);
    }

}
