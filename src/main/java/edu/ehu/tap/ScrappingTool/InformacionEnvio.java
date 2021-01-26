package edu.ehu.tap.ScrappingTool;

import java.util.Date;

/**
 * @author Eneko Ruiz
 */
public class InformacionEnvio {

    private String numeroAlbaran;
    private Date fechaEnvio;
    private String lugarEnvio;
    private Date fechaEntrega;
    private String lugarEntrega;

    public InformacionEnvio(String numeroAlbaran, Date fechaEnvio, String lugarEnvio, Date fechaEntrega, String lugarEntrega) {
        this.numeroAlbaran = numeroAlbaran;
        this.fechaEnvio = fechaEnvio;
        this.lugarEnvio = lugarEnvio;
        this.fechaEntrega = fechaEntrega;
        this.lugarEntrega = lugarEntrega;
    }

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

    public void setNumeroAlbaran(String numeroAlbaran) {
        if (this.numeroAlbaran == null) {
            this.numeroAlbaran = numeroAlbaran;
        }
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
