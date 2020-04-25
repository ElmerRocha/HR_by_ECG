package com.dye.heartratebyecg;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Grafica extends IOIOActivity {


    /*-------- Variables programables ---------*/
    final int Pin_ECG=46;//Este es el pin de entrada analogico
    final int Bateria_PIN=31;//Pin analogico para leer la tensión de la batería
    final long milisegundos = 5;//Este valor es la frecuencia con la que se tomará una muestra analogica
    //final long milisegundos = 4;//Registro CSV cu17
    //final long milisegundos = 8;//Registro CSV 803
    //final long milisegundos = 13;//Registro CSV 16265
    final int CantidadMuestrasUmbral = 200;//Es el numero de muestras que se tiene en cuenta para calcular el umbral.
    final int CantidadMuestras = (int) (6/(milisegundos/1000.0));//Este es el numero de muestras que verá en la grafica
    final int multiplicador = 100;//Este valor es con el normalizará la lectura analogica
    final int PorcentajeUmbral = 14;//Este valor es lo que se agrega para el umbral
    //final int PorcentajeUmbral = 30;//Prueba CSV
    final int QT = 400;//Duración promedio de segmento QT en ms.
    /*-----------------------------------------*/

    /*-------- Variables generales ------------*/
    private int conteo = 1;//Conteo para promediar la variabilidad de frecuencia
    private int ejeX = 0;//Esta variable llevará el desplazamiento en eje X de la grafica
    private int CantidadPulsos = 0;//Esta variable llevará el conteo de pulsos
    private double promedio;//Variables para el calculo de umbral.
    double umbral=50;
    private int ConteoMuestras=0;//Variable para el promedio que se lleva para calcular el umbral.
    private double RC_aprox=0;
    boolean eRR;//,desconectar;
    int Num = 1;
    //Texto
    private String txt_Nombre;
    private String txt_ID;
    private String txt_Edad;
    private String txt_Altura;
    private String txt_Peso;
    private String txt_Genero;
    /*-----------------------------------------*/

    /*------------ Variables PDF --------------*/
    private int TiempoInicial;
    private int TiempoFinal;
    private PdfDocument document = new PdfDocument();
    private boolean CrearPagina=true;
    private PdfDocument.PageInfo pageInfo;
    private PdfDocument.Page page;
    private Canvas PaginaCanvas;
    private int EspacioTexto;
    private int NumPagina=1;
    private int MargenExterno = 45;//Margen de 2.5 cm = 0.984252'' = 71 P.S.
    private int MargenLateral = 40;//Margen de 3.0 cm = 1.1811'' = 85 P.S.
    int AlturaImg = 232;//Altura de las imágenes
    private int pageWidth = 595;
    private int pageHeight = 842;
    private int i_n=1;
    private Paint ColorNegro = new Paint();
    OutputStream outputStream;
    private int ConteoPDF = 1;
    Bitmap Imagen;
    SimpleDateFormat fecha = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());//Formato de fecha

    File filepath = Environment.getExternalStorageDirectory();
    File dir = new File(filepath.getAbsolutePath() + "/PDF_ECG/");
    //File file = new File(dir, "ECG_"+fecha.format(new Date())+".pdf");
    File file = new File(dir, "ECG_"+fecha.format(new Date())+".pdf");
    /*-----------------------------------------*/

    /*-------- Variables temporizador ---------*/
    private CountDownTimer Temporizador;
    private static final long TiempoInicialTemporizador = 60000;//60.000 milisegundos son 60 segundos.
    private long TiempoEnMilisegundos = TiempoInicialTemporizador;
    private boolean Temporizador_contando;//Esta variable será la bandera del temporizador
    private boolean Graficando;//Esta variable será la bandera de la grafica
    /*-----------------------------------------*/

    /*---------- Variables de Views -----------*/
    //Texto
    private TextView TextoPulsos;
    private TextView TextoTemporizador;
    private TextView TextoRitmoCardiaco;
    //Botones
    private Button BotonTemporizador;
    private Button BotonReset;
    private Button BotonGraficar;
    private Button BotonGuardar;
    private Button BotonCompartir;
    //Grafica
    private GraphView Grafica;
    private LineGraphSeries<DataPoint> series;
    private PointsGraphSeries<DataPoint> RR;
    private LineGraphSeries<DataPoint> Lumb;
    //Layouts
    private ScrollView Layout1;
    private ScrollView Layout2;
    //EditText
    private EditText Nombre;
    private EditText ID;
    private EditText Edad;
    private EditText Altura;
    private EditText Peso;
    //RadioButton
    private RadioButton Femenino;
    private RadioButton Masculino;
    /*-----------------------------------------*/

    /*------------ Variables HRV --------------*/
    List<Integer> HRV = new ArrayList<>();
    int HRV_tmp=0;
    private int HRV_max,HRV_min,HRV_avg;
    private int HRV_Count;
    /*-----------------------------------------*/

    /*------------- Variables csv -------------*/
    //File dir_ecg = Environment.getExternalStorageDirectory();
    //Scanner scanner;
    /*-----------------------------------------*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Aceleracion de Hardware para la gráfica con GraphView
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_grafica);

        //Solicitud de permisos de escritura y lectura
        pedirPermisos();

        //Asociar Views con variables del sistema
        asociarViews();

        //Poner los datos de Sharedpreferences
        cargardatosSharedPreferences();
        //Ajustar GrapView
        personalizarGrapView();

        //Iniciar el temporizador
        actualizarTextoTemporizador();
    }//Cierra onCreate

    private AnalogInput Bateria;
    /**
     * Este es el hilo en el que ocurre toda la actividad IOIO. Se ejecutará
     * cada vez que la aplicación se reanuda y se cancela cuando se detiene.
     * Lo que esté en método setup() se llamará inmediatamente después de que se haya establecido una conexión con IOIO
     * Esto puede suceder varias veces. Entonces, loop() será llamada repetidamente hasta que el IOIO se desconecte.
     * Igual que en Arduino.
     */
    class Looper extends BaseIOIOLooper {
        private AnalogInput EntradaAnalogica;

        /**
         * Se llama cada vez que se establece una conexión con IOIO.
         * Normalmente se usa para abrir pines.
         * Se lanza ConnectionLostException cuando se pierde la conexión IOIO.
         */
        @Override
        protected void setup() {
            toast("¡IOIO se ha conectado!");
            try {
                EntradaAnalogica = ioio_.openAnalogInput(Pin_ECG);
                //Bateria = ioio_.openAnalogInput(Bateria_PIN);
                //803
                //scanner = new Scanner(new File(dir_ecg.getAbsolutePath() + "/ECG/ecg_supra.csv"));
                //cu17
                //scanner = new Scanner(new File(dir_ecg.getAbsolutePath() + "/ECG/ecg_taquiarritmia3.csv"));
                //r16265
                //scanner = new Scanner(new File(dir_ecg.getAbsolutePath() + "/ECG/ecg_normal.csv"));
            } catch (ConnectionLostException e) {
                e.printStackTrace();
            }
        }//Cierre setup

        //Variables
        long T1, T2;
        double RC_prom;
        double RC_temp;
        int cant = 10;
        double alfa = 0.35;
        double lectura,adc;
        long TiempoA,TiempoB,T_RR;//Variables para calcular la variabilidad de la frecuencia "el tiempo entre ondas R"
        /**
         * Llamado repetidamente mientras el IOIO está conectado.
         * En este método se programa lo que quiere que la tarjeta haga durante su ejecución.
         * Lanza ConnectionLostException cuando se pierde la conexión IOIO.
         * Lanza InterruptedException cuando el hilo IOIO ha sido interrumpido.
         */
        @Override
        public void loop() throws ConnectionLostException, InterruptedException {//throws InterruptedException, ConnectionLostException {
            //if(desconectar) ioio_.disconnect();
            try {
                if(Graficando) {//Se revisa la bandera, para saber si se está o no graficando
                    //adc = multiplicador * EntradaAnalogica.read();//Lectura analogica guardada en "lectura"
                    adc = 1.25*EntradaAnalogica.getVoltage();
                    //lectura = (alfa*adc) + ((1-alfa)*lectura);
                    lectura = lectura + alfa*(adc-lectura);
                    //alfa = Math.pow(2,-alfa);
                    //lectura = Float.parseFloat(scanner.nextLine());//Para leer los datos del archivo .csv
                    umbral = promedioUmbral(lectura);
                    //umbral = CalculoUmbral(lectura);

                    if (lectura > umbral) {
                        T1 = T2;
                        T2 = SystemClock.elapsedRealtime();//Devuelve el tiempo actual del reloj en milisegundos.
                        //Este if  detecta una onda R
                        if ( (T2-T1)> QT ) {//400 ms porque es el promedio de duración del segmento QT
                            TiempoA = TiempoB;
                            TiempoB = SystemClock.elapsedRealtime();
                            T_RR = TiempoB-TiempoA;
                            RC_temp = 60.0 / (T_RR/1000.0);//Se divide en 1k para que la medida quede en segundos.
                            RC_prom = RC_prom + RC_temp;
                            lectura+=0.35;//Chanchullo
                            if(conteo>=cant) {
                                RC_aprox = RC_prom / cant;
                                conteo = 1;
                                RC_prom = 0;
                            } else conteo++;
                            if (Temporizador_contando) {
                                CantidadPulsos++;
                                CalculoHRV((int)(T_RR), false);
                            }
                            //eRR=true;
                        }//If(T2-T1)
                    }//If(lectura>umbral)

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            series.appendData(new DataPoint(ejeX++, lectura), true, CantidadMuestras);//ejeX va aumentando cada que se agrega un dato.
                            //Lumb.appendData(new DataPoint(ejeX, umbral), true, CantidadMuestras);//ejeX va aumentando cada que se agrega un dato.
                        /*if(eRR) {
                            RR.appendData(new DataPoint(ejeX,lectura),true,CantidadMuestras);
                            eRR=false;
                        }*/
                            if (Temporizador_contando) ConteoPDF++;
                            if (ConteoPDF == Num*CantidadMuestras) {//If para ir agregando cosas al PDF
                                TiempoFinal = (int) ((TiempoInicialTemporizador - TiempoEnMilisegundos) / 1000);
                                if (TiempoInicial != TiempoFinal) AgregarDatosPDF(TiempoInicial, TiempoFinal);
                                TiempoInicial = TiempoFinal;
                                Num++;
                            }//if (ConteoPDF)
                        }
                    });

                    actualizarTextoGrafica(CantidadPulsos,RC_aprox);
                    //agregarEntradaGrafica(lectura);

                    Thread.sleep(milisegundos);//Aqui se hace un delay para que haga la lectura con una frecuencia especifica.
                }//If(graficando)

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }//Cierre loop
        //Método para mostrar cuando se desconecta IOIO
        @Override
        public void disconnected() {
            toast("¡IOIO se ha desconectado!");
            //scanner.close();
            //EntradaAnalogica.close();
            //Bateria.close();
        }

    }//Cierra Looper
    /*----------------------------- Métodos de IOIO -----------------------------*/
    @Override//El metodo para crear el hilo IOIO
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }
    //Metodo para imprimir mensajes en el celular tipo Toast, emergente.
    private void toast(final String mensaje) {
        final Context contexto = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(contexto, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }
    /*--------------------------------------------------------------------------*/

    /*--------------------- Método de asociación de Views ----------------------*/
    private void asociarViews() {
        /*--- Asociar las variables de Views con las views de la GUI ---*/
        //Texto
        TextoPulsos = findViewById(R.id.tv_Pulsos);
        TextoTemporizador = findViewById(R.id.tv_Temporizador);
        TextoRitmoCardiaco = findViewById(R.id.tv_RC);
        //Botones
        BotonTemporizador = findViewById(R.id.btn_Temporizador);
        BotonReset = findViewById(R.id.btn_ResetTempo);
        BotonGraficar = findViewById(R.id.btn_Graficar);
        BotonGuardar = findViewById(R.id.boton_guardar);
        BotonCompartir = findViewById(R.id.boton_compartir);
        //Grafica
        Grafica = findViewById(R.id.grafica);
        //EditTexts
        Nombre = findViewById(R.id.et_nombre);
        Edad = findViewById(R.id.et_edad);
        Altura = findViewById(R.id.et_altura);
        Peso = findViewById(R.id.et_peso);
        ID = findViewById(R.id.et_ID);
        //RadioButtons
        Femenino = findViewById(R.id.rb_F);
        Masculino = findViewById(R.id.rb_M);
        //Layouts
        Layout1 = findViewById(R.id.Layout1);
        Layout2 = findViewById(R.id.Layout2);
        /*--------------------------------------------------------------*/
    }
    /*--------------------------------------------------------------------------*/

    /*--------------- Método de permisos para escribir y leer ------------------*/
    private void pedirPermisos() {
        /*------- Permisos para escritura, lectura y Bluetooth --------*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
        } if( ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1000);
        }/* if( ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.BLUETOOTH},1000);
        }*/
        /*--------------------------------------------------------------*/
    }
    /*--------------------------------------------------------------------------*/

    /*----------------------------- Métodos de botones --------------------------*/
    @SuppressLint("SetTextI18n")
    public void BtnGraficar(View view) {
        if(Graficando) {
            Graficando = false;
            BotonGraficar.setText("Graficar");
        } else {
            Graficando = true;
            BotonGraficar.setText("Parar gráfica");
        }
    }
    public void BtnContarPulsos(View view) {
        if (Temporizador_contando) {
            pausarTemporizador();
        } else {
            empezarTemporizador();
        }

    }
    public void BtnResetTempo(View view) {
        resetTemporizador();
    }
    /*---------------------------------------------------------------------------*/

    /*------------------------ Métodos de temporizador --------------------------*/
    @SuppressLint("SetTextI18n")
    private void empezarTemporizador() {
        Temporizador = new CountDownTimer(TiempoEnMilisegundos, 1000) {//1000 milisegundos = 1 segundo.
            @Override
            public void onTick(long tiempo) {
                if (TiempoEnMilisegundos == TiempoInicialTemporizador) CantidadPulsos=0;
                TiempoEnMilisegundos = tiempo;
                actualizarTextoTemporizador();
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                Temporizador_contando = false;
                BotonTemporizador.setText("Contar pulsos");
                BotonTemporizador.setVisibility(View.INVISIBLE);
                BotonReset.setVisibility(View.VISIBLE);

                Graficando=false;
                ConteoPDF = 0;

                TiempoFinal = (int) ((TiempoInicialTemporizador - TiempoEnMilisegundos) / 1000);
                if (TiempoInicial != TiempoFinal) AgregarDatosPDF(TiempoInicial, TiempoFinal);
                BotonGuardar.setVisibility(View.VISIBLE);
                BotonGraficar.setText("Graficar");
                CalculoHRV(0, true);
            }
        }.start();

        Temporizador_contando=true;
        BotonTemporizador.setText("Pausar conteo");
        BotonReset.setVisibility(View.INVISIBLE);
    }
    @SuppressLint("SetTextI18n")
    private void pausarTemporizador() {
        Temporizador.cancel();
        Temporizador_contando = false;
        BotonTemporizador.setText("Reanudar conteo");
        BotonReset.setVisibility(View.VISIBLE);
    }
    @SuppressLint("SetTextI18n")
    private void resetTemporizador() {
        TiempoEnMilisegundos = TiempoInicialTemporizador;
        BotonTemporizador.setText("Contar pulsos");
        BotonReset.setVisibility(View.INVISIBLE);
        BotonTemporizador.setVisibility(View.VISIBLE);
        actualizarTextoTemporizador();

        BotonGuardar.setVisibility(View.INVISIBLE);

        document.finishPage(page);
        document.close();
        document = new PdfDocument();
    }
    private void actualizarTextoTemporizador() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int segundos = (int) TiempoEnMilisegundos / 1000;
                String tiempo = String.format(Locale.getDefault(),"%02d", segundos)+" s";
                if (segundos<10) tiempo = String.format(Locale.getDefault(),"%01d", segundos)+" s";
                TextoTemporizador.setText(tiempo);
            }
        });

    }
    /*---------------------------------------------------------------------------*/

    /*-------------------------- Método de umbral -------------------------------*/
    private double promedioUmbral(double lectura) {
        //Ciclo para calcular el umbral
        if (ConteoMuestras < CantidadMuestrasUmbral) {
            promedio = promedio + lectura;
            ConteoMuestras++;
        } else {
            umbral = (1+(PorcentajeUmbral/100.0))*(promedio/CantidadMuestrasUmbral);
            promedio=0;
            ConteoMuestras=0;
        }
        return umbral;
    }
//    List<Double> CalUmbral = new ArrayList<>();
//    double Um_max,Um_min;
//    double Umb_calc=0;
//    private double CalculoUmbral(final double dato) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if(CalUmbral.size() == CantidadMuestrasUmbral) {
//                    Um_max = Collections.max(CalUmbral);
//                    Um_min = Collections.min(CalUmbral);
//                    Umb_calc = (Um_max - Um_min) * (PorcentajeUmbral/100.0);
//                    CalUmbral.clear();
//                } else {
//                    CalUmbral.add(dato);
//                }
//            }
//        });
//        return Umb_calc;
//    }
    /*---------------------------------------------------------------------------*/

    /*---------------------- Método para encontrar HRV --------------------------*/
    private void CalculoHRV(final int TRR, final boolean bool) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bool) {
                    HRV_min = Collections.min(HRV);
                    HRV_max = Collections.max(HRV);
                    HRV_avg = HRV_tmp/(HRV.size());
                    HRV_Count = HRV.size();
                    HRV.clear();
                    HRV_tmp=0;
                } else {
                    HRV.add(TRR);
                    HRV_tmp+=TRR;
                }
            }
        });
    }
    /*---------------------------------------------------------------------------*/

    /*------------------------ Métodos de Gráfica -------------------------------*/
//    private void agregarEntradaGrafica(final double datoEntrada) {//Metodo para agregar datos a GraphView
//        //Se elige mostar maximo # puntos en el Viewpoint y que haga scroll hasta el final
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                series.appendData(new DataPoint(ejeX++, datoEntrada), true, CantidadMuestras);//ejeX va aumentando cada que se agrega un dato.
//                //Lumb.appendData(new DataPoint(ejeX, umbral), true, CantidadMuestras);//ejeX va aumentando cada que se agrega un dato.
//                if(eRR) {
//                    RR.appendData(new DataPoint(ejeX,datoEntrada),true,CantidadMuestras);
//                    eRR=false;
//                }
//                if (Temporizador_contando) ConteoPDF++;
//                if (ConteoPDF == Num*CantidadMuestras) {//If para ir agregando cosas al PDF
//                    TiempoFinal = (int) ((TiempoInicialTemporizador - TiempoEnMilisegundos) / 1000);
//                    if (TiempoInicial != TiempoFinal) AgregarDatosPDF(TiempoInicial, TiempoFinal);
//                    TiempoInicial = TiempoFinal;
//                    Num++;
//                }//if (ConteoPDF)
//            }
//        });
//
//    }
    private void actualizarTextoGrafica(int pulsos, double rc) {
        final String v1 = pulsos+" bpm";
        final String v2 = (int)rc + " bpm";//Se hace un cast de RC para convertirlo en entero.

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextoPulsos.setText(v1);
                TextoRitmoCardiaco.setText(v2);
            }
        });

    }
    private void personalizarGrapView() {
        /*---------- GraphView y personalización de la gráfica ---------*/
        //series = new LineGraphSeries<DataPoint>();
        series = new LineGraphSeries<>();
        RR = new PointsGraphSeries<>();
        Lumb = new LineGraphSeries<>();
        Grafica.addSeries(series);
        Grafica.addSeries(RR);
        Grafica.addSeries(Lumb);
        Lumb.setColor(Color.GREEN);
        RR.setColor(Color.RED);
        RR.setSize(8f);

        Viewport Grid = Grafica.getViewport();
        GridLabelRenderer Label = Grafica.getGridLabelRenderer();

        Grid.setXAxisBoundsManual(true);
        Grid.setYAxisBoundsManual(true);
        Grid.setMinX(0.0001);
        Grid.setMinY(0.0001);
        Grid.setMaxX(CantidadMuestras);
        Grid.setMaxY(3.3+0.0001);//Para lectura analógica
        //Grid.setMaxY(multiplicador+0.0001);//Para csv
        Grid.setScrollable(false);
        Grid.setScalable(false);

        Label.reloadStyles();
        Label.setGridColor(getResources().getColor(R.color.BlancoTransparente));
        Grid.setDrawBorder(true);

        Label.setHumanRounding(false,false);
        Label.setNumHorizontalLabels(151);
        Label.setNumVerticalLabels(101);
        Label.setVerticalLabelsVisible(false);
        Label.setHorizontalLabelsVisible(false);
        /*--------------------------------------------------------------*/
    }
    /*---------------------------------------------------------------------------*/

    /*------------ Metodos de creación y exportación del documento --------------*/
    private void AgregarDatosPDF(final int A, final int B) {
        //Obtener imagen
        Imagen = CapturarGrafica();

        //Texto para agregar
        String tmpTxt = A+"-"+B+" s:";

        if ( (EspacioTexto+AlturaImg) > (pageHeight-MargenExterno) ) {
            CrearPagina=true;
            NumPagina++;
            document.finishPage(page);
        }
        if (CrearPagina) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Crear un page description
                    //El formato A4 es 8.27 × 11.69 -> en P.S. es 595x842
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth,pageHeight, NumPagina).create();
                    //Empezar Documento
                    page = document.startPage(pageInfo);//page,
                    PaginaCanvas = page.getCanvas();
                    EspacioTexto = MargenExterno;
                    ColorNegro.setColor(Color.BLACK);//Color Negro
                    CrearPagina = false;
                }
            });
        }
        //Agregar texto
        PaginaCanvas.drawText(tmpTxt, MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=2;//Salto de linea
        //Agregar imagen
        PaginaCanvas.drawBitmap(Imagen, MargenLateral-5,EspacioTexto, new Paint());
        EspacioTexto+=AlturaImg;


    }//AgregarDatosPDF
    public void GuardarPDF(View view) {
        SharedPreferences Datos = getSharedPreferences("datos", Context.MODE_PRIVATE);
        txt_Nombre = Datos.getString("nombre", "");
        txt_ID = Datos.getString("ID","");
        txt_Edad = Datos.getString("edad", "");
        txt_Altura = Datos.getString("altura", "");
        txt_Peso = Datos.getString("peso", "");

        if(Datos.getBoolean("femenino",false)) txt_Genero="Femenino";
        if(Datos.getBoolean("masculino",false)) txt_Genero="Masculino";

        int HR_max_aprox = (int) Math.round(60.0/(HRV_max/1000.0));
        int HR_min_aprox = (int) Math.round(60.0/(HRV_min/1000.0));
        int HR_avg_aprox = (int) Math.round(60.0/(HRV_avg/1000.0));

        String HRVmax = HR_max_aprox+" bpm ("+HRV_max+" ms)";
        String HRVmin = HR_min_aprox+" bpm ("+HRV_min+" ms)";
        String HRVavg = HR_avg_aprox+" bpm ("+HRV_avg+" ms)";

        String diagnostico;
        if(CantidadPulsos>100) {
            diagnostico = "Taquicardia";
        } else if(CantidadPulsos<60) {
            diagnostico = "Bradicardia";
        } else {
            diagnostico = "Frecuencia cardíaca normal";
        }
        //Finaliza la pagina anterior
        document.finishPage(page);

        //Crea una nueva página
        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth,pageHeight, NumPagina+1).create();
        page = document.startPage(pageInfo);
        PaginaCanvas = page.getCanvas();
        EspacioTexto = MargenExterno;

        //Agregar texto
        PaginaCanvas.drawText("Información del usuario:", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=25;//Salto de linea
        PaginaCanvas.drawText("Nombre: "+txt_Nombre, MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Identificación: "+txt_ID, MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Edad: "+txt_Edad+" años", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Género: "+txt_Genero, MargenLateral, EspacioTexto, ColorNegro);
        //PaginaCanvas.drawText("Género: Desconocido", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Altura: "+txt_Altura+" cm", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Peso: "+txt_Peso+" kg", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=50;//Salto de linea
        PaginaCanvas.drawText("Resultados de la medición:", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=25;//Salto de linea
        PaginaCanvas.drawText("RC protocolo clínico: "+CantidadPulsos+" bpm", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Diagnóstico: "+diagnostico, MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=25;//Salto de linea
        PaginaCanvas.drawText("Variabilidad de frecuencia cardiaca: ", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Max: "+HRVmin, MargenLateral+7, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Min: "+HRVmax, MargenLateral+7, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("RC por VFC: "+HRVavg, MargenLateral+7, EspacioTexto, ColorNegro);
        //EspacioTexto+=15;//Salto de linea
        //PaginaCanvas.drawText("Cantidad de muestras HRV: "+HRV_Count, MargenLateral, EspacioTexto, ColorNegro);

        document.finishPage(page);

        //Si el directorio no existe, crearlo.
        if (!dir.exists()) dir.mkdir();

        try {
            outputStream = new FileOutputStream(file);
            document.writeTo(outputStream);
            document.close();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();

        }

        toast("Ducumento guardado en "+dir.getAbsolutePath()+" como "+file.getName());
        BotonGuardar.setVisibility(View.INVISIBLE);
        BotonCompartir.setVisibility(View.VISIBLE);
    }
    private Bitmap CapturarGrafica() {
        final Bitmap Captura = Bitmap.createBitmap(Grafica.getWidth(), Grafica.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas Img = new Canvas(Captura);
        Grafica.draw(Img);

        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                File directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File direct = new File(directorio.getAbsolutePath()+"/ECG_Imagenes/");
                if(!direct.exists()) direct.mkdir();
                File archivo = new File(direct,"ECG_"+i_n+".jpg");
                i_n++;
                try {
                    salida = new FileOutputStream(archivo);
                    Captura.compress(Bitmap.CompressFormat.JPEG, 100, salida);
                    salida.flush();
                    salida.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });*/

        Captura.setDensity(1100);
        return Captura;
    }
    public void Compartir(View view)  {
        //Después de guardar el archivo en el dispositivo

        /*Uri uri = Uri.fromFile(file);
        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.setType("application/pdf");
        share.putExtra(Intent.EXTRA_SUBJECT,file.getName());
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(share,"Compartir"));

        BotonCompartir.setVisibility(View.INVISIBLE);*/


        //Exportando - Compartiendo
        Context context = getApplicationContext();
        Uri path = FileProvider.getUriForFile(context, "com.dye.heartratebyecg.fileprovider", file);
        Intent fileIntent = new Intent(Intent.ACTION_SEND);
        fileIntent.setType("application/pdf");
        fileIntent.putExtra(Intent.EXTRA_SUBJECT, "PDF");
        fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        fileIntent.putExtra(Intent.EXTRA_STREAM, path);
        startActivity(Intent.createChooser(fileIntent, "Compartir"));

        //BotonCompartir.setVisibility(View.INVISIBLE);

    }
    /*---------------------------------------------------------------------------*/

    /*---------------- Método para guardar datos del usuario ---------------------*/
    public void BotonGuardar(final View view) throws ConnectionLostException, InterruptedException {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txt_Nombre = Nombre.getText().toString();
                txt_ID = ID.getText().toString();
                txt_Edad = Edad.getText().toString();
                txt_Altura = Altura.getText().toString();
                txt_Peso = Peso.getText().toString();

                if(Femenino.isChecked()) txt_Genero="Femenino";
                if(Masculino.isChecked()) txt_Genero="Masculino";

                if(txt_Nombre.isEmpty() || txt_Edad.isEmpty() || txt_Altura.isEmpty() ||
                        txt_Peso.isEmpty() || txt_Genero.isEmpty() || txt_ID.isEmpty()) {//(RGenero.getCheckedRadioButtonId() == -1)) {
                    toast("Por favor rellene todos los datos.");
                } else {

                    SharedPreferences pref = getSharedPreferences("datos", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("nombre", txt_Nombre);
                    editor.putString("ID",txt_ID);
                    editor.putString("edad", txt_Edad);
                    editor.putString("altura",txt_Altura);
                    editor.putString("peso", txt_Peso);
                    editor.putBoolean("femenino",Femenino.isChecked());
                    editor.putBoolean("masculino",Masculino.isChecked());
                    editor.apply();

                    Layout1.animate().translationY(Layout1.getHeight()).setDuration(1000);
                    Layout2.setVisibility(View.VISIBLE);
                    Layout1.setVisibility(View.GONE);
                }
            }
        });
        //VoltajeBateria();
    }
    private void cargardatosSharedPreferences() {
        /*------------------- Shared Preferences -----------------------*/
        SharedPreferences preferences = getSharedPreferences("datos", Context.MODE_PRIVATE);
        Nombre.setText(preferences.getString("nombre", ""));
        ID.setText(preferences.getString("ID",""));
        Edad.setText(preferences.getString("edad", ""));
        Altura.setText(preferences.getString("altura", ""));
        Peso.setText(preferences.getString("peso", ""));
        Femenino.setChecked(preferences.getBoolean("femenino",false));
        Masculino.setChecked(preferences.getBoolean("masculino",false));
        /*--------------------------------------------------------------*/
    }
    /*---------------------------------------------------------------------------*/

    /*------------- Método para medir tensión de la batería ---------------------*/
//    private void VoltajeBateria() throws ConnectionLostException, InterruptedException {
//        float VBat = Bateria.getVoltage();
//        String Bat = String.format(Locale.getDefault(),"%.3f", VBat);
//
//        //if(VBat < 2.7) {
//            AlertDialog.Builder alerta = new AlertDialog.Builder(this);
//            alerta.setTitle("Voltaje de batería bajo");
//            alerta.setMessage("Por favor recargue la batería. Vbat:"+Bat+"V");
//            alerta.setCancelable(false);
//            alerta.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    desconectar=true;
//                }
//            });
//            alerta.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                @Override
//                public void onCancel(DialogInterface dialogInterface) {
//                    desconectar=true;
//                }
//            });
//            alerta.create().show();
//        //}
//    }
    /*---------------------------------------------------------------------------*/

}//Cierra Grafica