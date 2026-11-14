package edu.campusbite.common.rmi;

import edu.campusbite.common.dto.PedidoCocina;
import java.rmi.Remote;
import java.rmi.RemoteException;

// --------------------- DAVID ---------
// OYE CARLOS AQUI TE DEJE EL METODO YA HACES TU NMMS
public interface ServicioCocina extends Remote {
    String NOMBRE = "ServicioCocina";

    void registrarPedido(PedidoCocina pedido) throws RemoteException, OperacionRechazadaException;
}
