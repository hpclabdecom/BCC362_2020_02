/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spendreport;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.walkthrough.common.sink.AlertSink;
import org.apache.flink.walkthrough.common.entity.Alert;
import org.apache.flink.walkthrough.common.entity.Transaction;
import org.apache.flink.walkthrough.common.source.TransactionSource;

/**
 * FraudDetectionJob class define o fluxo de dado da aplicação
 */
public class FraudDetectionJob {
	public static void main(String[] args) throws Exception {

		// Cria o ambiente de execução, local onde é "setada" as propriedades do Job
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		/* Define uma fonte de dados. Esses dados são gerados em sistemas externos
		 * como Kafka, Rabbit MQ e outros, e então inseridos em Flink Jobs
		 */
		DataStream<Transaction> transactions = env
			.addSource(new TransactionSource())
			.name("transactions");

		/* Como é gerado uma quantidade grande de transações/usuarios, as tarefas são executadas em paralelo
		 * porém deve ser garantido que as transações de um mesmo usuário sejam processadas pela mesma task criada.
		 * A função process() aplica a função keyby em cada partição da stream.
		 */
		DataStream<Alert> alerts = transactions
			.keyBy(Transaction::getAccountId) // Garante que um usuário seja tratado pela mesma task
			.process(new FraudDetector())
			.name("fraud-detector");

		alerts
			.addSink(new AlertSink())
			.name("send-alerts");

		env.execute("Fraud Detection");
	}
}
