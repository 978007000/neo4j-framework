/*
 * Copyright (c) 2013 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.runtime;

import com.graphaware.runtime.config.DefaultRuntimeConfiguration;
import com.graphaware.runtime.metadata.ProductionSingleNodeMetadataRepository;
import com.graphaware.runtime.metadata.ProductionSingleNodeMetadataRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.unsafe.batchinsert.TransactionSimulatingBatchGraphDatabase;

import java.io.IOException;

import static com.graphaware.runtime.config.RuntimeConfiguration.GA_METADATA;

/**
 * Unit test for {@link ProductionRuntime} used with batch graph database.
 */
public class BatchDatabaseRuntimeTest extends DatabaseBackedRuntimeTest {

    private final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        temporaryFolder.create();
        database = new TransactionSimulatingBatchGraphDatabase(BatchInserters.batchDatabase(temporaryFolder.getRoot().getAbsolutePath()), 1);
        repository = new ProductionSingleNodeMetadataRepository(database, DefaultRuntimeConfiguration.getInstance());
    }

    @After
    public void tearDown() {
        try {
            database.shutdown();
        } catch (IllegalStateException e) {
            //already shutdown = ok
        }

        temporaryFolder.delete();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotBeAllowedToDeleteRootNode() {
        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(database);
        runtime.start();

        getRuntimeRoot().delete();
    }

    protected Node getRuntimeRoot() {
        Node root = null;

        try (Transaction tx = database.beginTx()) {
            //deliberately using deprecated API, do not attempt to fix, or at least run the test afterwards
            //noinspection deprecation
            for (Node node : database.getAllNodes()) {
                if (node.hasLabel(GA_METADATA)) {
                    root = node;
                    break;
                }
            }

            tx.success();
        }

        return root;
    }

    protected Node createRuntimeRoot() {
        Node root;

        try (Transaction tx = database.beginTx()) {
            if (getRuntimeRoot() != null) {
                throw new IllegalArgumentException("Runtime root already exists!");
            }
            root = database.createNode(GA_METADATA);
            tx.success();
        }

        return root;
    }
}
