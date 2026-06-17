package com.colegio.asistencia.kafka;

import com.colegio.asistencia.dto.InasistenciaEvent;
import com.colegio.asistencia.entity.EstadoAsistencia;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integracion Kafka + Zookeeper.
 *
 * @EmbeddedKafka levanta un broker Kafka real en memoria junto con su Zookeeper coordinador.
 * Esto permite verificar el flujo completo productor -> topic -> consumidor
 * sin necesidad de infraestructura externa.
 *
 * Analogia tecnica: es como un banco de pruebas de motor que simula condiciones reales
 * de carga sin instalar el motor en el vehiculo definitivo.
 */
@SpringBootTest(
    // webEnvironment MOCK necesario: SecurityConfig requiere HttpSecurity (contexto servlet)
    // EmbeddedKafka funciona igual sin importar el webEnvironment
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EmbeddedKafka(
    partitions = 1,
    topics = { KafkaZookeeperIntegrationTest.TOPIC_INASISTENCIA },
    // brokerProperties configura el broker embebido (Zookeeper incluido internamente)
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:${embeddedKafka.port}",
        "auto.create.topics.enable=true",
        "log.dir=/tmp/kafka-tests-asistencia"
    }
)
@DirtiesContext   // Destruye el contexto al terminar para liberar el puerto
@ActiveProfiles("test")
class KafkaZookeeperIntegrationTest {

    /** Nombre del topic bajo prueba; debe coincidir con kafka.topics.inasistencia */
    static final String TOPIC_INASISTENCIA = "inasistencia.detectada";

    /** Consumer group exclusivo para los tests (no interfiere con el grupo de produccion) */
    private static final String GROUP_ID_TEST = "test-asistencia-group";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    // Cola thread-safe donde el MessageListener deposita los mensajes recibidos
    private BlockingQueue<ConsumerRecord<String, InasistenciaEvent>> mensajesRecibidos;
    private KafkaMessageListenerContainer<String, InasistenciaEvent> contenedorConsumidor;
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void configurar() {
        mensajesRecibidos = new LinkedBlockingQueue<>();

        // --- Configuracion del Productor ---
        // Se apunta al broker embebido que levanto @EmbeddedKafka
        Map<String, Object> propProductor = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        propProductor.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propProductor.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(propProductor));

        // --- Configuracion del Consumidor ---
        // JsonDeserializer necesita saber el tipo de valor esperado para deserializar
        Map<String, Object> propConsumidor = KafkaTestUtils.consumerProps(GROUP_ID_TEST, "true", embeddedKafkaBroker);
        propConsumidor.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propConsumidor.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        propConsumidor.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        propConsumidor.put(JsonDeserializer.VALUE_DEFAULT_TYPE, InasistenciaEvent.class.getName());

        DefaultKafkaConsumerFactory<String, InasistenciaEvent> factoriaConsumidor =
                new DefaultKafkaConsumerFactory<>(propConsumidor);

        ContainerProperties propContenedor = new ContainerProperties(TOPIC_INASISTENCIA);
        contenedorConsumidor = new KafkaMessageListenerContainer<>(factoriaConsumidor, propContenedor);

        // Registrar el listener que deposita mensajes en la cola de verificacion
        contenedorConsumidor.setupMessageListener(
                (MessageListener<String, InasistenciaEvent>) mensajesRecibidos::add
        );
        contenedorConsumidor.start();

        // Esperar a que el consumidor obtenga particiones asignadas antes de producir
        ContainerTestUtils.waitForAssignment(contenedorConsumidor, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void limpiar() {
        contenedorConsumidor.stop();
    }

    /**
     * Test 14: Verificar que el broker Kafka (con Zookeeper embebido) esta activo
     * y es accesible, validando el estado del cluster antes de producir mensajes.
     */
    @Test
    @DisplayName("broker_kafkaConZookeeper_estaActivoYAccesible")
    void broker_kafkaConZookeeper_estaActivoYAccesible() {
        // Assert directo sobre el broker embebido
        // getBrokersAsString retorna "host:port" del broker; si esta vacio, el broker no inicio
        String direccionBroker = embeddedKafkaBroker.getBrokersAsString();
        assertThat(direccionBroker)
                .as("El broker Kafka con Zookeeper debe estar activo y tener una direccion valida")
                .isNotBlank()
                .contains(":");

        // Verificar que el topic fue creado automaticamente al arrancar el contexto
        assertThat(embeddedKafkaBroker.getTopics())
                .as("El topic de inasistencias debe existir en el cluster embebido")
                .contains(TOPIC_INASISTENCIA);
    }

    /**
     * Test 15: Verificar el ciclo completo productor -> Kafka -> consumidor.
     * Publica un InasistenciaEvent en el topic y confirma que el consumidor lo recibe
     * con los datos intactos dentro del timeout configurado.
     */
    @Test
    @DisplayName("producirYConsumirEvento_flujoCicloCompleto_mensajeLlegaIntacto")
    void producirYConsumirEvento_flujoCicloCompleto_mensajeLlegaIntacto() throws InterruptedException {
        // Arrange: construir el evento que normalmente genera AsistenciaService
        InasistenciaEvent eventoEsperado = new InasistenciaEvent(
                201L,                           // asistenciaId
                42L,                            // alumnoId
                3L,                             // cursoId
                LocalDate.of(2026, 5, 14),      // fecha
                "Ausente sin justificacion"     // observacion
        );

        // Act: publicar el evento en el broker embebido (Kafka + Zookeeper)
        kafkaTemplate.send(TOPIC_INASISTENCIA, String.valueOf(eventoEsperado.alumnoId()), eventoEsperado);

        // Assert: esperar hasta 10 segundos a que el consumidor reciba el mensaje
        // En un broker real el RTT es <10ms; el timeout cubre latencia de arranque del test
        ConsumerRecord<String, InasistenciaEvent> registro =
                mensajesRecibidos.poll(10, TimeUnit.SECONDS);

        assertThat(registro)
                .as("El consumidor debe recibir el mensaje dentro del timeout")
                .isNotNull();

        // Verificar que la clave de particion es el alumnoId (para garantizar orden por alumno)
        assertThat(registro.key())
                .as("La clave del mensaje debe ser el alumnoId para garantizar orden")
                .isEqualTo("42");

        // Verificar integridad del payload deserializado
        InasistenciaEvent eventoRecibido = registro.value();
        assertThat(eventoRecibido.asistenciaId()).isEqualTo(201L);
        assertThat(eventoRecibido.alumnoId()).isEqualTo(42L);
        assertThat(eventoRecibido.cursoId()).isEqualTo(3L);
        assertThat(eventoRecibido.fecha()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(eventoRecibido.observacion()).isEqualTo("Ausente sin justificacion");
    }

    // -------------------------------------------------------------------------
    // Configuracion minima de Spring necesaria para el contexto del test.
    // Se declara aqui mismo para no contaminar el application.yml de produccion.
    // -------------------------------------------------------------------------
    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        // El contexto solo necesita los beans que provee @EmbeddedKafka.
        // KafkaTemplate y los factories se construyen manualmente en @BeforeEach
        // para tener control total sobre la configuracion del broker embebido.
    }
}
