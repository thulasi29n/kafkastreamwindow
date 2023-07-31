List<Branched<KStream<String, ValidatedTransaction>>> branches = 
    Stream.of(regulators).map(regulatorId -> 
        Branched.<String, ValidatedTransaction>as(regulatorId.toString())
            .withFunction(stream -> stream
                .join(globalKTable,
                    (key, value) -> key,
                    (validatedTransaction, referenceData) -> {
                        // Extract the countSize and triggerTime for each regulator from the reference data
                        int countSize = referenceData.getCountSize();
                        LocalTime triggerTime = referenceData.getTriggerTime();

                        // Instantiate the BatchTrackingProcessor with the extracted countSize and triggerTime
                        BatchTrackingProcessor batchProcessor = new BatchTrackingProcessor(countSize, triggerTime);

                        // Process each validatedTransaction with the batchProcessor
                        batchProcessor.process(key, validatedTransaction);

                        // Return the validatedTransaction to continue the stream
                        return validatedTransaction;
                    })
                .to("outputTopic_" + regulatorId.toString(), Produced.with(Serdes.String(), new ValidatedTransactionSerde())))
    )
    .collect(Collectors.toList());
