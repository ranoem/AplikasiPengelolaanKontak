import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class PengelolaanKontakFrame extends javax.swing.JFrame {
    
    public PengelolaanKontakFrame() {
        initComponents();
        pack();
        setLocationRelativeTo(null);
        connectToDatabase();
        loadDataToTable(contactTable);

        // Event Listeners
        btnSimpan.addActionListener(e -> addContact(nameField, phoneField, emailField, categoryBox));
        btnEdit.addActionListener(e -> editContact(nameField, phoneField, emailField, categoryBox, contactTable));
        btnHapus.addActionListener(e -> deleteContact(contactTable));
        btnCari.addActionListener(e -> searchContacts(contactTable, searchField));
        
        contactTable.getSelectionModel().addListSelectionListener(e -> {
            // Cek apakah ada baris yang dipilih
            if (!e.getValueIsAdjusting() && contactTable.getSelectedRow() != -1) {
                int selectedRow = contactTable.getSelectedRow();

                // Ambil data dari baris yang dipilih dan set ke JTextField
                nameField.setText(contactTable.getValueAt(selectedRow, 1).toString());  // Kolom 1 untuk 'name'
                phoneField.setText(contactTable.getValueAt(selectedRow, 2).toString());  // Kolom 2 untuk 'phone'
                emailField.setText(contactTable.getValueAt(selectedRow, 3).toString());  // Kolom 3 untuk 'email'
                categoryBox.setSelectedItem(contactTable.getValueAt(selectedRow, 4).toString());  // Kolom 4 untuk 'category'
            }
        });
        
        listKategori.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                // Ambil kategori yang dipilih
                String selectedCategory = listKategori.getSelectedValue();
                // Tampilkan data sesuai kategori
                updateTableData(selectedCategory);
            }
        });
        
        btnEkspor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveTableToCSV();
            }
        });

        btnImpor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Menampilkan JFileChooser untuk memilih file CSV
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Pilih File CSV untuk Diimpor");
                fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

                int userSelection = fileChooser.showOpenDialog(null);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToImport = fileChooser.getSelectedFile();
                    String filePath = fileToImport.getAbsolutePath();
                    importCSVToDatabase(filePath); // Panggil metode impor
                }
            }
        });
        
        // Add an ItemListener to the JComboBox
        categoryBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // Get the selected value from JComboBox
                    String selectedCategory = (String) categoryBox.getSelectedItem();

                    // Update the JList to reflect the selected category
                    listKategori.setSelectedValue(selectedCategory, true);
                }
            }
        });
    }
    
    private Connection connectToDatabase() {
        // Pastikan URL JDBC yang benar
        String url = "jdbc:sqlite:D:/sqlitedb/contact.db"; // Ganti dengan path database Anda
        try {
            // Memastikan driver SQLite terdeteksi
            Class.forName("org.sqlite.JDBC"); // Tambahkan baris ini untuk memastikan driver terload
            return DriverManager.getConnection(url);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Koneksi gagal: " + e.getMessage());
            return null; // Pastikan koneksi null jika gagal
        }
    }
    
    private void loadDataToTable(JTable contactTable) {
        String sql = "SELECT id, name, phone, email, category FROM contacts";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Phone", "Email", "Category"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Supaya tabel tidak bisa diedit langsung
                }
            };

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("name"), rs.getString("phone"),
                    rs.getString("email"), rs.getString("category")
                });
            }

            contactTable.setModel(model);
            contactTable.getColumnModel().getColumn(0).setMinWidth(0); // Sembunyikan kolom ID
            contactTable.getColumnModel().getColumn(0).setMaxWidth(0);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading contacts: " + e.getMessage());
        }
    }

    private boolean isValidPhone(String phone) {
        // Memastikan nomor telepon hanya terdiri dari angka dan panjangnya 12 digit
        return phone.matches("\\d{12}");
    }

    private void addContact(JTextField nameField, JTextField phoneField, JTextField emailField, JComboBox<String> categoryBox) {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String category = (String) categoryBox.getSelectedItem();

        // Validasi input
        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        // Validasi nomor telepon
        if (!isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 12 digits and contain only numbers.");
            return;
        }

        String sql = "INSERT INTO contacts (name, phone, email, category) VALUES (?, ?, ?, ?)";

        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, email);
            pstmt.setString(4, category);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Contact added successfully!");
            loadDataToTable(contactTable); // Refresh table
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding contact: " + e.getMessage());
        }
    }

    private void editContact(JTextField nameField, JTextField phoneField, JTextField emailField, JComboBox<String> categoryBox, JTable contactTable) {
        int selectedRow = contactTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a contact to edit.");
            return;
        }

        // Ambil ID dari tabel (kolom pertama)
        int contactId = Integer.parseInt(contactTable.getValueAt(selectedRow, 0).toString());

        // Ambil data baru dari input field
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String category = (String) categoryBox.getSelectedItem();

        // Validasi input
        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        // Validasi nomor telepon
        if (!isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 12 digits and contain only numbers.");
            return;
        }

        // Query SQL untuk update kontak
        String sql = "UPDATE contacts SET name = ?, phone = ?, email = ?, category = ? WHERE id = ?";

        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, email);
            pstmt.setString(4, category);
            pstmt.setInt(5, contactId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Contact updated successfully!");
                loadDataToTable(contactTable); // Refresh tabel setelah update
            } else {
                JOptionPane.showMessageDialog(this, "No changes were made.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating contact: " + e.getMessage());
        }
    }

    private void deleteContact(JTable contactTable) {
        int selectedRow = contactTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a contact to delete.");
            return;
        }

        // Dapatkan nama kontak untuk konfirmasi
        String contactName = contactTable.getValueAt(selectedRow, 1).toString(); // Asumsi kolom 1 adalah nama
        int confirm = JOptionPane.showConfirmDialog(
                this, 
                "Are you sure you want to delete the contact: " + contactName + "?", 
                "Confirm Deletion", 
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.NO_OPTION) {
            return; // Jika pengguna memilih "No", batalkan penghapusan
        }

        // Ambil ID dari tabel untuk eksekusi query
        int id = Integer.parseInt(contactTable.getValueAt(selectedRow, 0).toString());

        String sql = "DELETE FROM contacts WHERE id = ?";

        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Contact deleted successfully!");
            loadDataToTable(contactTable); // Refresh table
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting contact: " + e.getMessage());
        }

        clearFields();
    }

    public void searchContacts(JTable contactTable, JTextField searchField) {
        String searchQuery = searchField.getText();  // Ambil input dari searchField

        // Jika searchQuery kosong, muat seluruh data
        String query;
        if (searchQuery.trim().isEmpty()) {
            query = "SELECT * FROM contacts";  // Ambil seluruh data
        } else {
            query = "SELECT * FROM contacts WHERE name LIKE ? OR phone LIKE ?";  // Cari berdasarkan nama atau phone
        }

        try (Connection conn = connectToDatabase()) {
            if (conn == null) {
                System.out.println("Koneksi gagal, tidak bisa melakukan pencarian.");
                return; // Koneksi gagal, keluar dari metode
            }

            // Siapkan prepared statement dengan query
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                // Jika ada query pencarian, tambahkan parameter
                if (!searchQuery.trim().isEmpty()) {
                    stmt.setString(1, "%" + searchQuery + "%");
                    stmt.setString(2, "%" + searchQuery + "%");
                }

                // Eksekusi query
                ResultSet rs = stmt.executeQuery();

                // Dapatkan model tabel
                DefaultTableModel model = (DefaultTableModel) contactTable.getModel();
                model.setRowCount(0);  // Kosongkan tabel sebelumnya

                // Menambahkan hasil pencarian ke tabel
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("category")
                    };
                    model.addRow(row);  // Menambahkan baris ke tabel
                }

                rs.close();
            }
        } catch (SQLException e) {
            System.out.println("Error saat pencarian: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void clearFields() {
        nameField.setText("");   // Mengosongkan nameField
        phoneField.setText("");  // Mengosongkan phoneField
        emailField.setText("");  // Mengosongkan emailField
        categoryBox.setSelectedIndex(0); // Mengosongkan categoryField
    }
    
    // Metode untuk mengupdate tabel berdasarkan kategori yang dipilih
    public void updateTableData(String category) {
        // Buat query yang berbeda berdasarkan kategori
        String query = "SELECT * FROM contacts";

        // Jika kategori bukan 'All', tambahkan kondisi kategori pada query
        if (!category.equals("All")) {
            query += " WHERE category = ?";
        }

        try (Connection conn = connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            // Jika kategori bukan 'All', set parameter kategori
            if (!category.equals("All")) {
                stmt.setString(1, category);
            }

            ResultSet rs = stmt.executeQuery();

            // Set data ke dalam JTable
            DefaultTableModel tableModel = (DefaultTableModel) contactTable.getModel();
            tableModel.setRowCount(0); // Bersihkan data tabel sebelumnya

            while (rs.next()) {
                int id = rs.getInt("id"); // Ambil ID (tetap disimpan untuk kebutuhan internal)
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                String cat = rs.getString("category");

                // Tambahkan data ke tabel (ID dimasukkan namun tersembunyi)
                tableModel.addRow(new Object[]{id, name, phone, email, cat});
            }

            // Sembunyikan kolom ID dalam JTable karena pada database sudah auto increment
            if (contactTable.getColumnModel().getColumnCount() > 0) {
                contactTable.getColumnModel().getColumn(0).setMinWidth(0);
                contactTable.getColumnModel().getColumn(0).setMaxWidth(0);
                contactTable.getColumnModel().getColumn(0).setWidth(0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void saveTableToCSV() {
        // Membuat JFileChooser untuk memilih lokasi dan nama file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Data ke CSV");
        fileChooser.setSelectedFile(new File("kontak.csv")); // Menentukan nama default file

        // Mengatur filter untuk hanya menunjukkan file CSV
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

        // Menampilkan dialog penyimpanan file
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();

            // Memastikan file berakhiran .csv
            if (!filePath.endsWith(".csv")) {
                filePath += ".csv";
            }

            // Menyimpan data tabel ke file CSV
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                DefaultTableModel model = (DefaultTableModel) contactTable.getModel();

                // Menulis header kolom ke file CSV
                writer.write("Nama;Nomor Telepon;Email;Kategori");
                writer.newLine();

                // Menulis data tabel ke file CSV
                for (int i = 0; i < model.getRowCount(); i++) {
                    String nama = model.getValueAt(i, 1).toString();
                    String phone = model.getValueAt(i, 2).toString();
                    String email = model.getValueAt(i, 3).toString();
                    String category = model.getValueAt(i, 4).toString();

                    // Menulis setiap data baris ke file CSV
                    writer.write(nama + ";");
                    writer.write(phone + ";");
                    writer.write(email + ";");
                    writer.write(category + ";");
                    writer.newLine();
                }

                JOptionPane.showMessageDialog(null, "Data berhasil disimpan ke file: " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat menyimpan data.");
            }
        }
    }
   
    private void insertContactToDatabase(String name, String phone, String email, String category) {
        String sql = "INSERT INTO contacts (name, phone, email, category) VALUES (?, ?, ?, ?)";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, email);
            pstmt.setString(4, category);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat menyimpan data kontak.");
        }
    }

    private void importCSVToDatabase(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            DefaultTableModel model = (DefaultTableModel) contactTable.getModel();

            // Skip header (if exists)
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] data = line.split(";");

                if (data.length != 4) {
                    JOptionPane.showMessageDialog(null, "Data di baris " + lineNumber + " tidak lengkap.");
                    continue;
                }

                String name = data[0].trim();
                String phone = data[1].trim();
                String email = data[2].trim();
                String category = data[3].trim();

                // Validasi nomor telepon agar tidak duplikat
                if (isPhoneDuplicate(phone)) {
                    JOptionPane.showMessageDialog(null, "Nomor telepon " + phone + " sudah ada di database. Baris ke-" + lineNumber + " dilewati.");
                    continue; // Skip kontak dengan nomor telepon yang duplikat
                }

                // Menyisipkan data ke database
                String sql = "INSERT INTO contacts (name, phone, email, category) VALUES (?, ?, ?, ?)";
                try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, phone);
                    pstmt.setString(3, email);
                    pstmt.setString(4, category);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat menyimpan data kontak.");
                }
            }

            JOptionPane.showMessageDialog(null, "Impor CSV selesai.");
            loadDataToTable(contactTable); // Refresh tabel setelah impor
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat membaca file CSV.");
        }
    }

    private boolean isPhoneDuplicate(String phone) {
        String sql = "SELECT COUNT(*) FROM contacts WHERE phone = ?";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Jika ada duplikasi
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Tidak ada duplikasi
    }

  

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        phoneField = new javax.swing.JTextField();
        emailField = new javax.swing.JTextField();
        categoryBox = new javax.swing.JComboBox<>();
        btnSimpan = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listKategori = new javax.swing.JList<>();
        jLabel7 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        searchField = new javax.swing.JTextField();
        btnCari = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        contactTable = new javax.swing.JTable();
        btnEkspor = new javax.swing.JButton();
        btnImpor = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Aplikasi Pengelolaan Kontak");

        jPanel1.setBackground(new java.awt.Color(219, 139, 138));

        jLabel1.setFont(new java.awt.Font("Trebuchet MS", 1, 21)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Aplikasi Pengelolaan Kontak");

        jPanel2.setBackground(new java.awt.Color(219, 139, 138));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));

        jLabel2.setFont(new java.awt.Font("Trebuchet MS", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Nama");

        jLabel3.setFont(new java.awt.Font("Trebuchet MS", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("No Telepon");

        jLabel4.setFont(new java.awt.Font("Trebuchet MS", 1, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Email");

        jLabel5.setFont(new java.awt.Font("Trebuchet MS", 1, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Kategori");

        categoryBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Pilih", "Teman", "Keluarga", "Rekan Kerja", "Profesional", "Jasa", "Tetangga", "Akademik", "Kontak Darurat", "Pelanggan/Klien" }));

        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSimpanActionPerformed(evt);
            }
        });

        btnEdit.setText("Edit");

        btnHapus.setText("Hapus");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 87, Short.MAX_VALUE)
                        .addComponent(btnSimpan, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(14, 14, 14)
                        .addComponent(btnHapus, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5))
                        .addGap(13, 13, 13)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(nameField)
                            .addComponent(phoneField)
                            .addComponent(emailField)
                            .addComponent(categoryBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(19, 19, 19))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(phoneField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(emailField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(categoryBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSimpan)
                    .addComponent(btnEdit)
                    .addComponent(btnHapus))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jPanel3.setBackground(new java.awt.Color(219, 139, 138));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));

        listKategori.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "All", "Teman", "Keluarga", "Rekan Kerja", "Profesional", "Jasa", "Tetangga", "Akademik", "Kontak Darurat", "Pelanggan/Klien" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(listKategori);

        jLabel7.setFont(new java.awt.Font("Trebuchet MS", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("List Kategori");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(0, 150, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(219, 139, 138));
        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));

        btnCari.setText("Cari");

        contactTable.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(contactTable);

        btnEkspor.setText("Ekspor CSV");
        btnEkspor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEksporActionPerformed(evt);
            }
        });

        btnImpor.setText("Impor CSV");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 584, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 490, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnCari, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(217, 217, 217)
                        .addComponent(btnEkspor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnImpor)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEkspor)
                    .addComponent(btnImpor))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(182, 182, 182))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSimpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSimpanActionPerformed

    private void btnEksporActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEksporActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnEksporActionPerformed

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
            java.util.logging.Logger.getLogger(PengelolaanKontakFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PengelolaanKontakFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PengelolaanKontakFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PengelolaanKontakFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PengelolaanKontakFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnEkspor;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnImpor;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JComboBox<String> categoryBox;
    private javax.swing.JTable contactTable;
    private javax.swing.JTextField emailField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList<String> listKategori;
    private javax.swing.JTextField nameField;
    private javax.swing.JTextField phoneField;
    private javax.swing.JTextField searchField;
    // End of variables declaration//GEN-END:variables
}
