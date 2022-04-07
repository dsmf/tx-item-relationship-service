//
// Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
//
// See the AUTHORS file(s) distributed with this work for additional
// information regarding authorship.
//
// See the LICENSE file(s) distributed with this work for
// additional information regarding license terms.
//
package net.catenax.irs.connector.consumer.service;


import lombok.RequiredArgsConstructor;
import net.catenax.irs.connector.consumer.configuration.ConsumerConfiguration;
import net.catenax.irs.connector.job.MultiTransferJob;
import net.catenax.irs.connector.job.RecursiveJobHandler;
import net.catenax.irs.connector.requests.JobsTreeRequest;
import net.catenax.irs.connector.util.JsonUtil;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.util.stream.Stream;

/**
 * Implementation of {@link RecursiveJobHandler} that retrieves
 * parts trees from potentially multiple calls to IRS API behind
 * multiple EDC Providers, and assembles their outputs into
 * one overall parts tree.
 */
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement") // Monitor doesn't offer guard statements
public class JobsTreeRecursiveJobHandler implements RecursiveJobHandler {

    /**
     * Logger.
     */
    private final Monitor monitor;
    /**
     * Storage account name.
     */
    private final ConsumerConfiguration configuration;
    /**
     * Json Converter.
     */
    private final JsonUtil jsonUtil;
    /**
     * Recursive retrieval logic implementation.
     */
    private final JobsTreeRecursiveLogic logic;

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<DataRequest> initiate(final MultiTransferJob job) {
        monitor.info("Initiating recursive retrieval for Job " + job.getJobId());
        final JobsTreeRequest jobsTreeRequest = getJobsTreeRequest(job);
        return logic.createInitialPartsTreeRequest(jobsTreeRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<DataRequest> recurse(final MultiTransferJob job, final TransferProcess transferProcess) {
        monitor.info("Proceeding with recursive retrieval for Job " + job.getJobId());

        final var requestTemplate = getJobsTreeRequest(job);
        return logic.createSubsequentPartsTreeRequests(transferProcess, requestTemplate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete(final MultiTransferJob job) {
        monitor.info("Completed retrieval for Job " + job.getJobId());
        final var completedTransfers = job.getCompletedTransfers();
        final var targetBlobName = job.getJobData().get(ConsumerService.DESTINATION_PATH_KEY);
        logic.assemblePartialPartTreeBlobs(completedTransfers, targetBlobName);
    }

    private JobsTreeRequest getJobsTreeRequest(final MultiTransferJob job) {
        final var partsTreeRequestAsString = job.getJobData().get(ConsumerService.PARTS_REQUEST_KEY);
        return jsonUtil.fromString(partsTreeRequestAsString, JobsTreeRequest.class);
    }
}
