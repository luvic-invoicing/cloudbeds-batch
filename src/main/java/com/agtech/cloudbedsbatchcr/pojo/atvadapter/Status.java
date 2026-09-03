package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

//num": ["recibido", "procesando", "aceptado", "rechazado", "error"]
public enum Status
{
    CREATED( "created" ), //Ready to be send to Hacienda First time
    AUTHORIZED( "aceptado" ), //Authorized by Hacienda
    NO_AUTHORIZED( "rechazado" ), //Failed on Hacienda Authorization
    IN_PROGRESS( "procesando" ),  //Send to Hacienda - waiting for reply
    RECEIVED( "recibido" ),
    ERROR( "error" ), //Failed to send to Hacienda
    REVERTED( "anulada" ), //Authorized Invoice and Reverted with Credit Memo
    FORWARDED( "forwarded" ), //NO-AUTHORIZED && FORWARDED - OR - AUTHORIZED && REVERTED && FORWARDED
    UNDEFINED( "indefinido" ), //Ready to be send to Hacienda First time
    NOT_FOUND( "no existe en hacienda" ), //no existe en hacienda
    BAD_REQUEST( "Datos inválidos" ), //no existe en hacienda
    DOWN_HACIENDA( "down" ); //when  Hacienda is down

    private final String code;

    Status( String code )
    {
        this.code = code;
    }

    public String toString()
    {
        return code;
    }


    public static Status valueOfCode( String code )
    {
        for ( Status e : values() )
        {
            if ( e.code.equalsIgnoreCase( code ) )
            {
                return e;
            }
        }
        return null;
    }
}
