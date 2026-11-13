package edu.campusbite.common.rmi;

/**
 * Error de negocio que viaja por RMI hasta el cliente.
 * Es distinto de RemoteException: aqui el servicio SI respondio,
 * pero rechazo la operacion (datos invalidos o conflicto).
 */
public class OperacionRechazadaException extends Exception {

    private static final long serialVersionUID = 1L;

    public OperacionRechazadaException(String mensaje) {
        super(mensaje);
    }
}
