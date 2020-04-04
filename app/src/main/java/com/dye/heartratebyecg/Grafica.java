package com.dye.heartratebyecg;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.view.View;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Grafica extends IOIOActivity {

    /*-------- Variables programables ---------*/
    final int Pin_ECG=40;//Este es el pin de entrada analogico
    final long milisegundos = 20;//Este valor es la frecuencia con la que se tomará una muestra analogica
    final int CantidadMuestrasUmbral = 40;//Es el numero de muestras que se tiene en cuenta para calcular el umbral.
    final int CantidadMuestras = 300;//Este es el numero de muestras que verá en la grafica
    final int multiplicador = 100;//Este valor es con el normalizará la lectura analogica
    /*-----------------------------------------*/

    /*-------- Variables generales ------------*/
    private int conteo = 1;//Conteo para promediar la variabilidad de frecuencia
    private int ejeX = 0;//Esta variable llevará el desplazamiento en eje X de la grafica
    private int CantidadPulsos = 0;//Esta variable llevará el conteo de pulsos
    private double promedio;//Variables para el calculo de umbral.
    private double umbral=50;
    private int ConteoMuestras=0;//Variable para el promedio que se lleva para calcular el umbral.
    private double RC_aprox=0;
    //Texto
    private String txt_Nombre;
    private String txt_Edad;
    private String txt_Altura;
    private String txt_Peso;
    private String txt_Genero;
    //PDF
    OutputStream outputStream;
    private int ConteoPDF = 1;
    private Bitmap Imagen;
    SimpleDateFormat fecha = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());//Formato de fecha
    /*-----------------------------------------*/

    /*-------- Variables temporizador ---------*/
    private CountDownTimer Temporizador;
    private static final long TiempoInicialTemporizador = 30000;//60.000 milisegundos son 60 segundos.
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
    //Grafica
    private GraphView Grafica;
    private LineGraphSeries<DataPoint> series;
    //Layouts
    private ScrollView Layout1;
    private ScrollView Layout2;
    //EditText
    private EditText Nombre;
    private EditText Edad;
    private EditText Altura;
    private EditText Peso;
    //RadioButton
    private RadioButton Femenino;
    private RadioButton Masculino;
    /*-----------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grafica);

        /*------- Permisos para escritura, lectura y Bluetooth --------*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
        } if( ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1000);
        } if( ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.BLUETOOTH},1000);
        }
        /*--------------------------------------------------------------*/

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
        //Grafica
        Grafica = findViewById(R.id.grafica);
        //EditTexts
        Nombre = findViewById(R.id.et_nombre);
        Edad = findViewById(R.id.et_edad);
        Altura = findViewById(R.id.et_altura);
        Peso = findViewById(R.id.et_peso);
        //RadioButtons
        Femenino = findViewById(R.id.rb_F);
        Masculino = findViewById(R.id.rb_M);
        //Layouts
        Layout1 = findViewById(R.id.Layout1);
        Layout2 = findViewById(R.id.Layout2);
        /*--------------------------------------------------------------*/

        /*------------------- Shared Preferences -----------------------*/
        SharedPreferences preferences = getSharedPreferences("datos", Context.MODE_PRIVATE);
        Nombre.setText(preferences.getString("nombre", ""));
        Edad.setText(preferences.getString("edad", ""));
        Altura.setText(preferences.getString("altura", ""));
        Peso.setText(preferences.getString("peso", ""));
        Femenino.setChecked(preferences.getBoolean("femenino",false));
        Masculino.setChecked(preferences.getBoolean("masculino",false));
        /*--------------------------------------------------------------*/

        /*---------- GraphView y personalización de la gráfica ---------*/
        //series = new LineGraphSeries<DataPoint>();
        series = new LineGraphSeries<>();
        Grafica.addSeries(series);

        Viewport Grid = Grafica.getViewport();
        GridLabelRenderer Label = Grafica.getGridLabelRenderer();
        Label.setGridColor(getResources().getColor(R.color.GrisOscuro));
        Grid.setDrawBorder(true);

        Grid.setXAxisBoundsManual(true);
        Grid.setYAxisBoundsManual(true);
        Grid.setMinX(0.0001);
        Grid.setMinY(0.0001);
        Grid.setMaxX(CantidadMuestras);
        Grid.setMaxY(multiplicador);
        Grid.setScrollable(false);

        Label.setNumHorizontalLabels(CantidadMuestras);
        Label.setNumVerticalLabels(multiplicador);
        Label.setVerticalLabelsVisible(false);
        Label.setHorizontalLabelsVisible(false);

        Label.reloadStyles();
        /*--------------------------------------------------------------*/


        //Aceleracion de Hardware para la gráfica con GraphView
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        //Iniciar el temporizador
        actualizarTextoTemporizador();
    }//Cierra onCreate

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
        protected void setup() throws ConnectionLostException {
            toast("¡IOIO se ha conectado!");
            try {
                EntradaAnalogica = ioio_.openAnalogInput(Pin_ECG);
            } catch (ConnectionLostException e) {
                ioio_.disconnect();
                throw e;
            }
        }//Cierre setup

        //Variables
        long T1, T2;
        double RC_prom;
        double RC_temp;
        int cant = 10;
        int Num=1;
        int TiempoInicial;
        int TiempoFinal;
        double lectura;
        long TiempoA,TiempoB;//Variables para calcular la variabilidad de la frecuencia "el tiempo entre ondas R"
        /**
         * Llamado repetidamente mientras el IOIO está conectado.
         * En este método se programa lo que quiere que la tarjeta haga durante su ejecución.
         * Lanza ConnectionLostException cuando se pierde la conexión IOIO.
         * Lanza InterruptedException cuando el hilo IOIO ha sido interrumpido.
         */
        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            try {
                if(Graficando) {//Se revisa la bandera, para saber si se está o no graficando
                    lectura = multiplicador * EntradaAnalogica.read();//Lectura analogica guardada en "lectura"
                    umbral = promedioUmbral(lectura);

                    if (lectura > umbral) {
                        T1 = T2;
                        T2 = SystemClock.elapsedRealtime();//Devuelve el tiempo actual del reloj en milisegundos.
                        //Este if  detecta una onda R
                        if (((T2-T1)>400) && Temporizador_contando) {//400 ms porque es el promedio de duración del segmento QT
                            TiempoA = TiempoB;
                            TiempoB = SystemClock.elapsedRealtime();
                            RC_temp = 60.0 / ((TiempoB - TiempoA) / 1000.0);//Se divide en 1k para que la medida quede en segundos.
                            RC_prom = RC_prom + RC_temp;
                            if(TiempoA!=0) CalculoHRV((int)(TiempoB-TiempoA), false);
                            if(conteo>=cant) {
                                RC_aprox = RC_prom / cant;
                                conteo = 1;
                                RC_prom = 0;
                            } else {
                                conteo++;
                            }
                            CantidadPulsos++;
                            //if (Temporizador_contando) CantidadPulsos++;
                        }
                    }//If(lectura>umbral)

                    //If para ir agregando cosas al PDF
                    if( (ConteoPDF >= Num*CantidadMuestras) || (ConteoPDF == 20090805) ) {

                        if(ConteoPDF == 20090805) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ConteoPDF=0;
                                    BotonGuardar.setVisibility(View.VISIBLE);
                                    Graficando=false;
                                    BotonGraficar.setText("Graficar");
                                    CalculoHRV(0, true);
                                }
                            });

                        }
                        TiempoFinal=(int)(TiempoInicialTemporizador-TiempoEnMilisegundos)/1000;
                        if(TiempoInicial != TiempoFinal) {
                            AgregarDatosPDF(TiempoInicial,TiempoFinal);
                        }
                        TiempoInicial=TiempoFinal;
                        Num++;
                    }//if (ConteoPDF)


                    actualizarTextoGrafica(CantidadPulsos,RC_aprox);
                    agregarEntradaGrafica(lectura);
                    Thread.sleep(milisegundos);//Aqui se hace un delay para que haga la lectura con una frecuencia especifica.
                }//If(graficando)

            } catch (ConnectionLostException e) {
                ioio_.disconnect();
                throw e;
            }
        }//Cierre loop
        //Método para mostrar cuando se desconecta IOIO
        @Override
        public void disconnected() {
            toast("¡IOIO se ha desconectado!");
            EntradaAnalogica.close();
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

    /*----------------------------- Métodos de botones --------------------------*/
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
    private void empezarTemporizador() {
        Temporizador = new CountDownTimer(TiempoEnMilisegundos, 1000) {//1000 milisegundos = 1 segundo.
            @Override
            public void onTick(long tiempo) {
                if (TiempoEnMilisegundos == TiempoInicialTemporizador) CantidadPulsos=0;
                TiempoEnMilisegundos = tiempo;
                actualizarTextoTemporizador();
            }

            @Override
            public void onFinish() {
                Temporizador_contando = false;
                BotonTemporizador.setText("Contar pulsos");
                BotonTemporizador.setVisibility(View.INVISIBLE);
                BotonReset.setVisibility(View.VISIBLE);
                ConteoPDF = 20090805;
            }
        }.start();

        Temporizador_contando=true;
        BotonTemporizador.setText("Pausar conteo");
        BotonReset.setVisibility(View.INVISIBLE);
    }
    private void pausarTemporizador() {
        Temporizador.cancel();
        Temporizador_contando = false;
        BotonTemporizador.setText("Reanudar conteo");
        BotonReset.setVisibility(View.VISIBLE);
    }
    private void resetTemporizador() {
        TiempoEnMilisegundos = TiempoInicialTemporizador;
        BotonTemporizador.setText("Contar pulsos");
        BotonReset.setVisibility(View.INVISIBLE);
        BotonTemporizador.setVisibility(View.VISIBLE);
        actualizarTextoTemporizador();

        BotonGuardar.setVisibility(View.INVISIBLE);
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
            umbral = 1.2*(promedio/CantidadMuestrasUmbral);
            promedio=0;
            ConteoMuestras=0;
        }
        return umbral;
    }
    /*---------------------------------------------------------------------------*/

    /*---------------------- Método para encontrar HRV --------------------------*/
    List<Long> HRV = new ArrayList<>();
    long HRV_tmp;
    private long HRV_max,HRV_min,HRV_avg;
    private void CalculoHRV(final long TRR, final boolean bool) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bool) {
                    HRV_min = Collections.min(HRV);
                    HRV_max = Collections.max(HRV);
                    for(Long i : HRV) HRV_tmp+=i;
                    HRV_avg = HRV_tmp/(HRV.size());
                    HRV.clear();
                } else HRV.add(TRR);
            }
        });
    }
    /*---------------------------------------------------------------------------*/

    /*------------------------ Métodos de Gráfica -------------------------------*/
    private void agregarEntradaGrafica(final double datoEntrada) {//Metodo para agregar datos a GraphView
        //Se elige mostar maximo # puntos en el Viewpoint y que haga scroll hasta el final
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                series.appendData(new DataPoint(ejeX++, datoEntrada), true, CantidadMuestras);//ejeX va aumentando cada que se agrega un dato.
                if(Temporizador_contando) ConteoPDF++;
            }
        });

    }
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
    /*---------------------------------------------------------------------------*/

    /*-------------------- Compartir el CSV-----------------------*/
    /*public void ExportarCSV(View view) throws IOException {

        //Imagen = getViewBitmap(Grafica);

        Imagen = Bitmap.createBitmap(Grafica.getWidth(),Grafica.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(Imagen);
        Grafica.draw(canvas);

        File filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File dir = new File(filepath.getAbsolutePath()+"/Coco/");
        dir.mkdir();
        File file = new File(dir,"Cocos.jpg");
        File file2 = new File(dir,"datos.csv");
        try {
            outputStream = new FileOutputStream(file);
            outputStream2 = new FileOutputStream(file2);

            Imagen.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream2.write((Datos.toString()).getBytes());

            outputStream.flush();
            //outputStream2.flush();
            outputStream.close();
            outputStream2.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        }
        toast("Imagen y archivo csv en "+dir.getAbsolutePath());


        try{

            //Guardar el archivo en el dispositivo
            FileOutputStream out = openFileOutput("data.csv", Context.MODE_PRIVATE);
            out.write((Datos.toString()).getBytes());
            out.close();

            //Exportando - Compartiendo
            Context context = getApplicationContext();
            File filelocation = new File(getFilesDir(), "data.csv");
            Uri path = FileProvider.getUriForFile(context, "com.dye.heartratebyecg.fileprovider", filelocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Data");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Guardar y compartir"));
        }
        catch(Exception e){
            e.printStackTrace();
        }

        BotonGuardar.setVisibility(View.INVISIBLE);
    }*/
    /*------------------ Metodos de prueba para guadar PDF ----------------------*/
    /*public void PDF(View view) throws IOException {
        //Crear un nuevo documento
        PdfDocument document = new PdfDocument();

        // Crear un page description
        //Las medidas son porque se utiliza el lenguaje PostScript que usa 1/72 de pulgada para las medidas
        //El formato A4 es 8.27 × 11.69 -> en P.S. es 595x842
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        //Empezar Documento
        PdfDocument.Page page = document.startPage(pageInfo);//Página 1
        Canvas canvas = page.getCanvas();//Se obtiene la pagina en canvas para mapearla como una imagen

        int margen=45;
        int mIzq = 40;
        int Altura = 232;

        Paint negro = new Paint();negro.setColor(Color.BLACK);

        canvas.drawText("Daniela hizo posible esto",mIzq,margen,negro);
        margen+=2;

        Imagen = Tomar();
        canvas.drawBitmap(Imagen, mIzq-5,margen, new Paint());
        margen+=Altura;

        canvas.drawText("Fue por ella",mIzq,margen,negro);
        margen+=2;

        Bitmap Imagen2 = Tomar();
        canvas.drawBitmap(Imagen2, mIzq-5,margen, new Paint());
        margen+=Altura;

        canvas.drawText("Y para ella",mIzq,margen,negro);
        margen+=2;

        Bitmap Imagen3 = Tomar();
        canvas.drawBitmap(Imagen3, mIzq-5,margen, new Paint());

        document.finishPage(page);

        File filepath = Environment.getExternalStorageDirectory();
        File dir = new File(filepath.getAbsolutePath() + "/PDF/");
        if (!dir.exists()) dir.mkdir();//Si el directorio no existe, crearlo.
        File file = new File(dir, "Prueba.pdf");

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

    }

    private Bitmap Tomar() {
        Bitmap Captura = Bitmap.createBitmap(Grafica.getWidth(), Grafica.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas Img = new Canvas(Captura);
        Grafica.draw(Img);
        Captura.setDensity(1100);
        return Captura;
    }*/
    /*---------------------------------------------------------------------------*/

    /*------------ Metodos de creación y exportación del documento --------------*/
    private PdfDocument document = new PdfDocument();
    private boolean CrearPagina=true;
    private PdfDocument.PageInfo pageInfo;
    private PdfDocument.Page page;
    private Canvas PaginaCanvas;
    private int EspacioTexto;
    private int NumPagina=1;
    private int MargenExterno = 45;//Margen de 2.5 cm = 0.984252'' = 71 P.S.
    private int MargenLateral = 40;//Margen de 3.0 cm = 1.1811'' = 85 P.S.
    private int AlturaImg = 232;//Altura de las imágenes
    private int pageWidth = 595;
    private int pageHeight = 842;
    private Paint ColorNegro = new Paint();

    private void AgregarDatosPDF(final int A, final int B) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

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
                //Agregar texto
                PaginaCanvas.drawText(tmpTxt, MargenLateral, EspacioTexto, ColorNegro);
                EspacioTexto+=2;//Salto de linea
                //Agregar imagen
                PaginaCanvas.drawBitmap(Imagen, MargenLateral-5,EspacioTexto, new Paint());
                EspacioTexto+=AlturaImg;
            }//Run
        });//Runnable

    }//AgregarDatosPDF
    public void GuardarPDF(View view) {
        SharedPreferences Datos = getSharedPreferences("datos", Context.MODE_PRIVATE);
        txt_Nombre = Datos.getString("nombre", "");
        txt_Edad = Datos.getString("edad", "");
        txt_Altura = Datos.getString("altura", "");
        txt_Peso = Datos.getString("peso", "");

        if(Datos.getBoolean("femenino",false)) txt_Genero="Femenino";
        if(Datos.getBoolean("masculino",false)) txt_Genero="Masculino";

        String HRVmax = HRV_max+" ms ("+HRV_max/60+") bpm";
        String HRVmin = HRV_min+" ms ("+HRV_min/60+") bpm";
        String HRVavg = HRV_avg+" ms ("+HRV_avg/60+") bpm";

        String HRV_txt= "Max: "+HRVmax+", Min: "+HRVmin+", Prom: "+HRVavg;


        //Agregar texto
        PaginaCanvas.drawText("Resultados:", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=25;//Salto de linea
        PaginaCanvas.drawText("Nombre: "+txt_Nombre, MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Edad: "+txt_Edad+" años", MargenLateral, EspacioTexto, ColorNegro);
        PaginaCanvas.drawText("Género: "+txt_Genero, MargenLateral+95, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Altura: "+txt_Altura+" cm", MargenLateral, EspacioTexto, ColorNegro);
        PaginaCanvas.drawText("Peso: "+txt_Peso+" kg", MargenLateral+95, EspacioTexto, ColorNegro);
        EspacioTexto+=25;//Salto de linea
        PaginaCanvas.drawText("Frecuencia cardiaca: "+CantidadPulsos+" bpm", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText("Variabilidad de frecuencia cardiaca: ", MargenLateral, EspacioTexto, ColorNegro);
        EspacioTexto+=15;//Salto de linea
        PaginaCanvas.drawText(HRV_txt, MargenLateral+10, EspacioTexto, ColorNegro);


        document.finishPage(page);




        File filepath = Environment.getExternalStorageDirectory();
        File dir = new File(filepath.getAbsolutePath() + "/PDF/");
        if (!dir.exists()) dir.mkdir();//Si el directorio no existe, crearlo.
        File file = new File(dir, "ECG_"+fecha.format(new Date())+".pdf");

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
    }
    private Bitmap CapturarGrafica() {
        Bitmap Captura = Bitmap.createBitmap(Grafica.getWidth(), Grafica.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas Img = new Canvas(Captura);
        Grafica.draw(Img);
        Captura.setDensity(1100);
        return Captura;
    }
    /*---------------------------------------------------------------------------*/

    /*---------------- Método para guardar datos del usuario ---------------------*/
    public void BotonGuardar(final View view) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txt_Nombre = Nombre.getText().toString();
                    txt_Edad = Edad.getText().toString();
                    txt_Altura = Altura.getText().toString();
                    txt_Peso = Peso.getText().toString();

                    if(Femenino.isChecked()) txt_Genero="Femenino";
                    if(Masculino.isChecked()) txt_Genero="Masculino";

                    if(txt_Nombre.isEmpty() || txt_Edad.isEmpty() || txt_Altura.isEmpty() ||
                       txt_Peso.isEmpty() || txt_Genero.isEmpty()) {//(RGenero.getCheckedRadioButtonId() == -1)) {
                        toast("Por favor rellene todos los datos.");
                    } else {

                        SharedPreferences pref = getSharedPreferences("datos", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("nombre", txt_Nombre);
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
    }
    /*---------------------------------------------------------------------------*/



}//Cierra Grafica

