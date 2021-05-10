package edu.ehu.tap.ScrappingTool;

import java.util.Date;

/**
 * Clase que contendra en sus atributos la informacion para cada envio
 * @author Eneko Ruiz
 */
public class InformacionEnvio {

    private final String numeroAlbaran;
    private Date fechaEnvio;
    private String lugarEnvio;
    private Date fechaEntrega;
    private String lugarEntrega;

    /**
     * Constructor con todos los parametros
     * @param numeroAlbaran Numero de albaran del envio
     * @param fechaEnvio Fecha de envio
     * @param lugarEnvio Lugar (Ciudad) de envio
     * @param fechaEntrega Fecha de entrega
     * @param lugarEntrega Lugar (ciudad) de entrega
     */
    public InformacionEnvio(String numeroAlbaran, Date fechaEnvio, String lugarEnvio, Date fechaEntrega, String lugarEntrega) {
        this.numeroAlbaran = numeroAlbaran;
        this.fechaEnvio = fechaEnvio;
        this.lugarEnvio = lugarEnvio;
        this.fechaEntrega = fechaEntrega;
        this.lugarEntrega = lugarEntrega;
    }

    /**
     * Constructor solo a partir del numero de albaran
     * @param numeroAlbaran Numero de albaran del envio
     */
    public InformacionEnvio(String numeroAlbaran) {
        this.numeroAlbaran = numeroAlbaran;
    }

    public String getNumeroAlbaran() {
        return numeroAlbaran;
    }

    public Date getFechaEnvio() {
        return fechaEnvio;
    }

    public String getLugarEnvio() {
        return lugarEnvio;
    }

    public Date getFechaEntrega() {
        return fechaEntrega;
    }

    public String getLugarEntrega() {
        return lugarEntrega;
    }


    public void setFechaEnvio(Date fechaEnvio) {
        if (this.fechaEnvio == null) {
            this.fechaEnvio = fechaEnvio;
        }
    }

    public void setLugarEnvio(String lugarEnvio) {
        if (this.lugarEnvio == null) {
            this.lugarEnvio = lugarEnvio;
        }
    }

    public void setFechaEntrega(Date fechaEntrega) {
        if (this.fechaEntrega == null) {
            this.fechaEntrega = fechaEntrega;
        }
    }

    public void setLugarEntrega(String lugarEntrega) {
        if (this.lugarEntrega == null) {
            this.lugarEntrega = lugarEntrega;
        }
    }
}
