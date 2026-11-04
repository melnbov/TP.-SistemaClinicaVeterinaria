package main;

import modelo.servicios.EmailNotificador;
import modelo.servicios.Notificador;
import vista.MenuConsola;

/** Punto de entrada de la aplicación. Es el único lugar que elige la implementación concreta del Notificador. */
public class Main {
    public static void main(String[] args) {
        Notificador notificador = new EmailNotificador(); // para avisar por SMS: new SmsNotificador()
        new MenuConsola(notificador).iniciar();
    }
}
