package cl.duoc.pedidos360.mqadmin.dto;

/** Reglas de nombres compartidas por los DTO de entrada. */
public final class Names {

    /** Letras, números, punto, guion y guion bajo; sin el prefijo "amq." que RabbitMQ reserva. */
    public static final String NAME_PATTERN = "^(?!amq\\.)[A-Za-z0-9._-]+$";
    public static final String NAME_MESSAGE =
            "solo admite letras, números, '.', '_' y '-', y no puede comenzar con 'amq.' (reservado por RabbitMQ)";
    public static final int NAME_MAX = 200;

    /** Routing key: además admite los comodines * y # de los exchanges topic. Puede ser vacía (fanout). */
    public static final String ROUTING_KEY_PATTERN = "^[A-Za-z0-9._*#-]*$";
    public static final String ROUTING_KEY_MESSAGE = "solo admite letras, números, '.', '_', '-', '*' y '#'";
    public static final int ROUTING_KEY_MAX = 255;

    private Names() {
    }
}
