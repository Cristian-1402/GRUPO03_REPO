/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package grupo03;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author User
 */
public class VentanaFISEI extends javax.swing.JFrame {
    Conexion con = new Conexion();
    
    /**
     * Creates new form VentanaFISEI
     */
    public VentanaFISEI() {
        initComponents();
        this.setLocationRelativeTo(null);
        CargarCombos();
        CargarTablaEstudiantes();
        CargarTablaAsistenciasSesion();
        CargarTablaReporte();
    }

    // ==========================================
    // MÉTODOS DE INICIALIZACIÓN
    // ==========================================
    public void CargarCombos() {
        String[] parciales = {"Parcial 1", "Parcial 2"};
        jcbxParcial.setModel(new DefaultComboBoxModel<>(parciales));
        jcbxParcialReporte.setModel(new DefaultComboBoxModel<>(parciales));

        String[] sesiones = {"1", "2", "3", "4", "5"};
        jcbxSesion.setModel(new DefaultComboBoxModel<>(sesiones));
    }

    // ==========================================
    // PESTAÑA 1: MATRICULACIÓN DE ESTUDIANTES
    // ==========================================
   public void CargarTablaEstudiantes() {
    String[] titulos = {"ID", "UID Llavero", "Cédula", "Nombres", "Apellidos"};
    DefaultTableModel modelo = new DefaultTableModel(null, titulos);
    String sql = "SELECT id, uid_rfid, cedula, nombres, apellidos FROM estudiantes ORDER BY id DESC";

    try (Connection cn = con.conectar();
         Statement st = cn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {

        while (rs.next()) {
            modelo.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("uid_rfid"),
                rs.getString("cedula"),
                rs.getString("nombres"),
                rs.getString("apellidos")
            });
        }
        jTable1.setModel(modelo); // Asigna los datos a jTable1
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al listar en tabla: " + e.getMessage());
    }
}
    
    public void GuardarEstudiante() {
    String cedula = jtxtCedula.getText().trim();
    String nombres = jtxtNombre.getText().trim();
    String apellidos = jtxtApellidos.getText().trim();
    // Limpia caracteres no numericos que envia el lector
    String uid = jtxtRfidMatricula.getText().replaceAll("[^0-9]", "").trim();

    // 1. Validar campos
    if (cedula.isEmpty() || nombres.isEmpty() || apellidos.isEmpty() || uid.isEmpty()) {
        JOptionPane.showMessageDialog(this, 
            "Debe llenar Cédula, Nombres, Apellidos y pasar el llavero por el lector.", 
            "Campos incompletos", 
            JOptionPane.WARNING_MESSAGE);
        return;
    }

    // 2. Intentar guardar en la tabla estudiantes
    String sql = "INSERT INTO estudiantes (uid_rfid, cedula, nombres, apellidos) VALUES (?, ?, ?, ?)";
    
    try (Connection cn = con.conectar()) {
        if (cn == null) {
            JOptionPane.showMessageDialog(this, "No hay conexión con la base de datos en XAMPP.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, uid);
            ps.setString(2, cedula);
            ps.setString(3, nombres);
            ps.setString(4, apellidos);
            
            int filasAfectadas = ps.executeUpdate();
            
            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "¡Estudiante registrado exitosamente!");
                LimpiarMatricula();
                CargarTablaEstudiantes(); // Recarga jTable1 inmediatamente
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, 
            "Error al registrar en MySQL:\n" + e.getMessage(), 
            "Error SQL", 
            JOptionPane.ERROR_MESSAGE);
    }
}
    

    public void LimpiarMatricula() {
        jtxtCedula.setText("");
        jtxtNombre.setText("");
        jtxtApellidos.setText("");
        jtxtRfidMatricula.setText("");
        jtxtCedula.requestFocus();
    }

    public void EliminarEstudiante() {
        int fila = jTable1.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante de la tabla para eliminar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = Integer.parseInt(jTable1.getValueAt(fila, 0).toString());
        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Seguro que desea eliminar al estudiante y desvincular su llavero?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);

        if (confirmacion == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM estudiantes WHERE id = ?";
            try (Connection cn = con.conectar(); 
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
                CargarTablaEstudiantes();
                JOptionPane.showMessageDialog(this, "Estudiante eliminado correctamente.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // PESTAÑA 2: CONTROL DE ASISTENCIA (RFID)
    // ==========================================
    public void CargarTablaAsistenciasSesion() {
        if (jcbxParcial.getSelectedItem() == null || jcbxSesion.getSelectedItem() == null) return;

        String parcial = jcbxParcial.getSelectedItem().toString();
        int sesion = Integer.parseInt(jcbxSesion.getSelectedItem().toString());

        String[] titulos = {"Cédula", "Estudiante", "Hora", "Estado"};
        DefaultTableModel modelo = new DefaultTableModel(null, titulos);

        String sql = "SELECT e.cedula, CONCAT(e.nombres, ' ', e.apellidos) AS alumno, a.hora, a.estado "
                   + "FROM asistencias a "
                   + "INNER JOIN estudiantes e ON a.id_estudiante = e.id "
                   + "WHERE a.parcial = ? AND a.sesion = ? "
                   + "ORDER BY a.hora DESC";

        try (Connection cn = con.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, parcial);
            ps.setInt(2, sesion);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getString("cedula"),
                    rs.getString("alumno"),
                    rs.getString("hora"),
                    rs.getString("estado")
                });
            }
            jtblMarcacionesSesion.setModel(modelo);

        } catch (Exception e) {
            System.err.println("Error al cargar asistencias de sesión: " + e.getMessage());
        }
    }

    public void RegistrarMarcacionRfid() {
        // Limpia caracteres no numéricos al pasar el llavero
        String uid = jtxtLectorRfid.getText().replaceAll("[^0-9]", "").trim();
        if (uid.isEmpty()) return;

        String parcial = jcbxParcial.getSelectedItem().toString();
        int sesion = Integer.parseInt(jcbxSesion.getSelectedItem().toString());

        String sqlBuscar = "SELECT id, nombres, apellidos FROM estudiantes WHERE uid_rfid = ?";
        String sqlInsertar = "INSERT INTO asistencias (id_estudiante, parcial, sesion, fecha, hora, estado) "
                           + "VALUES (?, ?, ?, CURDATE(), CURTIME(), 'PRESENTE')";

        try (Connection cn = con.conectar();
             PreparedStatement psBuscar = cn.prepareStatement(sqlBuscar)) {

            psBuscar.setString(1, uid);
            ResultSet rs = psBuscar.executeQuery();

            if (rs.next()) {
                int idEstudiante = rs.getInt("id");
                String nombreCompleto = rs.getString("nombres") + " " + rs.getString("apellidos");

                try (PreparedStatement psInsertar = cn.prepareStatement(sqlInsertar)) {
                    psInsertar.setInt(1, idEstudiante);
                    psInsertar.setString(2, parcial);
                    psInsertar.setInt(3, sesion);
                    psInsertar.executeUpdate();

                    jlblMensajeAsistencia.setText("Registrado: " + nombreCompleto + " (Sesión " + sesion + ")");
                    CargarTablaAsistenciasSesion();

                } catch (Exception exDuplicado) {
                    jlblMensajeAsistencia.setText("Aviso: " + nombreCompleto + " ya registró asistencia en la sesión " + sesion);
                }

            } else {
                jlblMensajeAsistencia.setText("Error: Llavero (" + uid + ") no matriculado.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en lectura RFID: " + e.getMessage());
        } finally {
            jtxtLectorRfid.setText("");
            jtxtLectorRfid.requestFocus();
        }
    }

    // ==========================================
    // PESTAÑA 3: REPORTE DE ASISTENCIA Y ESTADOS
    // ==========================================
    public void CargarTablaReporte() {
        String parcialSeleccionado = jcbxParcialReporte.getSelectedItem() != null 
                                     ? jcbxParcialReporte.getSelectedItem().toString() 
                                     : "Parcial 1";

        String[] titulos = {"Cédula", "Estudiante", "Asistencias", "% Asistencia", "Estado"};
        DefaultTableModel modelo = new DefaultTableModel(null, titulos);

        String sql = "SELECT e.cedula, CONCAT(e.nombres, ' ', e.apellidos) AS estudiante, "
                   + "COUNT(a.id) AS total_asistencias "
                   + "FROM estudiantes e "
                   + "LEFT JOIN asistencias a ON e.id = a.id_estudiante AND a.parcial = ? AND a.estado = 'PRESENTE' "
                   + "GROUP BY e.id, e.cedula, e.nombres, e.apellidos";

        try (Connection cn = con.conectar(); 
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, parcialSeleccionado);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int asistencias = rs.getInt("total_asistencias");
                double porcentaje = (asistencias / 5.0) * 100.0;

                String estado;
                if (porcentaje >= 80.0) {
                    estado = "Aprobado";
                } else if (porcentaje >= 70.0) {
                    estado = "En Riesgo / Supletorio";
                } else {
                    estado = "Reprobado por Faltas";
                }

                modelo.addRow(new Object[]{
                    rs.getString("cedula"),
                    rs.getString("estudiante"),
                    asistencias + " / 5",
                    String.format("%.1f%%", porcentaje),
                    estado
                });
            }
            jtblReporteFinal.setModel(modelo);
        } catch (Exception e) {
            System.err.println("Error al cargar reporte: " + e.getMessage());
        }
    }

    public void ImprimirReporteJasper() {
        try (Connection cn = con.conectar()) {
            String rutaReporte = "src/grupo03/reporte_asistencia.jasper";
            JasperPrint jp = JasperFillManager.fillReport(rutaReporte, null, cn);
            JasperViewer visor = new JasperViewer(jp, false);
            visor.setTitle("Reporte de Asistencia - FISEI");
            visor.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al generar reporte Jasper: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jtxtCedula = new javax.swing.JTextField();
        jtxtNombre = new javax.swing.JTextField();
        jtxtApellidos = new javax.swing.JTextField();
        jtxtRfidMatricula = new javax.swing.JTextField();
        jbtnGuardarEstudiante = new javax.swing.JButton();
        jbtnNuevoEstudiante = new javax.swing.JButton();
        jbtnEliminarEstudiante = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jcbxParcial = new javax.swing.JComboBox<>();
        jcbxSesion = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jtxtLectorRfid = new javax.swing.JTextField();
        jlblMensajeAsistencia = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtblMarcacionesSesion = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jcbxParcialReporte = new javax.swing.JComboBox<>();
        jbtnCalcularReporte = new javax.swing.JButton();
        jbtnImprimirJasper = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtblReporteFinal = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Cedula");

        jLabel2.setText("Nombres");

        jLabel3.setText("Apellidos ");

        jLabel4.setText("ID_Matricula");

        jtxtNombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jtxtNombreActionPerformed(evt);
            }
        });

        jbtnGuardarEstudiante.setText("Guardar");
        jbtnGuardarEstudiante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnGuardarEstudianteActionPerformed(evt);
            }
        });

        jbtnNuevoEstudiante.setText("Nuevo");
        jbtnNuevoEstudiante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnNuevoEstudianteActionPerformed(evt);
            }
        });

        jbtnEliminarEstudiante.setText("Eliminar");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jtxtNombre)
                                    .addComponent(jtxtApellidos)
                                    .addComponent(jtxtCedula))
                                .addGap(145, 145, 145)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jbtnGuardarEstudiante)
                                    .addComponent(jbtnNuevoEstudiante)
                                    .addComponent(jbtnEliminarEstudiante))
                                .addGap(53, 53, 53))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jtxtRfidMatricula, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jtxtCedula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnGuardarEstudiante))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jtxtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnNuevoEstudiante))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jtxtApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnEliminarEstudiante))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jtxtRfidMatricula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Matriculacion", jPanel1);

        jcbxParcial.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jcbxParcial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbxParcialActionPerformed(evt);
            }
        });

        jcbxSesion.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jcbxSesion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbxSesionActionPerformed(evt);
            }
        });

        jLabel5.setText("\"Acercar Llavero\"");

        jtxtLectorRfid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jtxtLectorRfidActionPerformed(evt);
            }
        });

        jtblMarcacionesSesion.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jtblMarcacionesSesion);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jcbxParcial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jcbxSesion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jtxtLectorRfid, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jlblMensajeAsistencia, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(97, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jcbxParcial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcbxSesion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jtxtLectorRfid, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jlblMensajeAsistencia, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Asistencia RFID", jPanel2);

        jcbxParcialReporte.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jcbxParcialReporte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbxParcialReporteActionPerformed(evt);
            }
        });

        jbtnCalcularReporte.setText("Calcular Reporte");
        jbtnCalcularReporte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnCalcularReporteActionPerformed(evt);
            }
        });

        jbtnImprimirJasper.setText("Imprimir");
        jbtnImprimirJasper.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnImprimirJasperActionPerformed(evt);
            }
        });

        jtblReporteFinal.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jtblReporteFinal);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jcbxParcialReporte, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(jbtnCalcularReporte)
                        .addGap(65, 65, 65)
                        .addComponent(jbtnImprimirJasper)))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jcbxParcialReporte, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnCalcularReporte)
                    .addComponent(jbtnImprimirJasper))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(7, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Reporte y Estados", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jbtnCalcularReporteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnCalcularReporteActionPerformed
        CargarTablaReporte();
        // TODO add your handling code here:
    }//GEN-LAST:event_jbtnCalcularReporteActionPerformed

    private void jtxtNombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtxtNombreActionPerformed
        GuardarEstudiante();
        // TODO add your handling code here:
    }//GEN-LAST:event_jtxtNombreActionPerformed

    private void jbtnGuardarEstudianteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnGuardarEstudianteActionPerformed
        GuardarEstudiante();
        // TODO add your handling code here:
    }//GEN-LAST:event_jbtnGuardarEstudianteActionPerformed

    private void jbtnNuevoEstudianteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnNuevoEstudianteActionPerformed
        EliminarEstudiante();
        // TODO add your handling code here:
    }//GEN-LAST:event_jbtnNuevoEstudianteActionPerformed

    private void jtxtLectorRfidActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtxtLectorRfidActionPerformed
        
        RegistrarMarcacionRfid();
        // TODO add your handling code here:
    }//GEN-LAST:event_jtxtLectorRfidActionPerformed

    private void jcbxParcialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbxParcialActionPerformed
        CargarTablaAsistenciasSesion();
        // TODO add your handling code here:
    }//GEN-LAST:event_jcbxParcialActionPerformed

    private void jcbxSesionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbxSesionActionPerformed
        CargarTablaAsistenciasSesion();
        // TODO add your handling code here:
    }//GEN-LAST:event_jcbxSesionActionPerformed

    private void jbtnImprimirJasperActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnImprimirJasperActionPerformed
        ImprimirReporteJasper();
        // TODO add your handling code here:
    }//GEN-LAST:event_jbtnImprimirJasperActionPerformed

    private void jcbxParcialReporteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbxParcialReporteActionPerformed
        CargarTablaReporte();
        // TODO add your handling code here:
    }//GEN-LAST:event_jcbxParcialReporteActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(VentanaFISEI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(VentanaFISEI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VentanaFISEI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VentanaFISEI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VentanaFISEI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton jbtnCalcularReporte;
    private javax.swing.JButton jbtnEliminarEstudiante;
    private javax.swing.JButton jbtnGuardarEstudiante;
    private javax.swing.JButton jbtnImprimirJasper;
    private javax.swing.JButton jbtnNuevoEstudiante;
    private javax.swing.JComboBox<String> jcbxParcial;
    private javax.swing.JComboBox<String> jcbxParcialReporte;
    private javax.swing.JComboBox<String> jcbxSesion;
    private javax.swing.JLabel jlblMensajeAsistencia;
    private javax.swing.JTable jtblMarcacionesSesion;
    private javax.swing.JTable jtblReporteFinal;
    private javax.swing.JTextField jtxtApellidos;
    private javax.swing.JTextField jtxtCedula;
    private javax.swing.JTextField jtxtLectorRfid;
    private javax.swing.JTextField jtxtNombre;
    private javax.swing.JTextField jtxtRfidMatricula;
    // End of variables declaration//GEN-END:variables
}
