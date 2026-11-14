package edu.campusbite.common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

// --------------------- DAVID ---------

// AQUI SE GUARDAN LOS PEDIDOS
public interface ServicioEstadoPedidos extends Remote {
    String NOMBRE = "ServicioEstadoPedidos";

    boolean marcarListo(String pedidoId) throws RemoteException, OperacionRechazadaException;
}
