package edu.campusbite.common.rmi;

import edu.campusbite.common.dto.EventoPedidoListo;
import java.rmi.Remote;
import java.rmi.RemoteException;

// --------------------- DAVID ---------
public interface ServicioNotificaciones extends Remote {
    String NOMBRE = "ServicioNotificaciones";

    void notificarListo(EventoPedidoListo evento) throws RemoteException, OperacionRechazadaException;
}
