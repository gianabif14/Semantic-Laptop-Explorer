package id.ac.semanticlaptop;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class LaptopFrame extends JFrame {
 private final LaptopRepository repository;
 private final JComboBox<String> brandBox = new JComboBox<>();
 private final JComboBox<Integer> ramBox = new JComboBox<>(new Integer[]{0, 8, 16, 32});
 private final JTextField maxPrice = new JTextField("40000000", 10);
 private final DefaultTableModel tableModel = new DefaultTableModel(
  new String[]{"Model", "Brand", "Processor", "RAM (GB)", "Storage", "Harga (IDR)", "Layar", "GPU", "OS"}, 0) {
  @Override public boolean isCellEditable(int r, int c) { return false; }
 };

 public LaptopFrame(LaptopRepository repository) {
  super("Semantic Laptop Explorer"); this.repository = repository;
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setSize(1180, 620); setLocationRelativeTo(null);
  buildUi(); loadBrands(); runSearch();
 }
 private void buildUi() {
  JPanel root = new JPanel(new BorderLayout(12, 12));
  root.setBorder(BorderFactory.createEmptyBorder(16,16,16,16)); root.setBackground(new Color(248,247,243));
  JLabel title = new JLabel("Laptop Specification Explorer");
  title.setFont(new Font("SansSerif", Font.BOLD, 23)); title.setForeground(new Color(35,65,95));
  JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
  filters.setBackground(Color.WHITE); filters.setBorder(BorderFactory.createTitledBorder("Filter SPARQL"));
  filters.add(new JLabel("Brand:")); filters.add(brandBox);
  filters.add(new JLabel("RAM minimum:")); filters.add(ramBox);
  filters.add(new JLabel("Harga maksimum (Rp):")); filters.add(maxPrice);
  JButton search = new JButton("Cari / Filter"); search.addActionListener(e -> runSearch()); filters.add(search);
  JTable table = new JTable(tableModel); table.setAutoCreateRowSorter(true); table.setRowHeight(26); table.setFillsViewportHeight(true);
  JPanel top = new JPanel(new BorderLayout(0,10)); top.setOpaque(false);
  top.add(title, BorderLayout.NORTH); top.add(filters, BorderLayout.CENTER);
  root.add(top, BorderLayout.NORTH); root.add(new JScrollPane(table), BorderLayout.CENTER); setContentPane(root);
 }
 private void loadBrands() {
  brandBox.addItem("Semua brand"); for (String b : repository.findBrands()) brandBox.addItem(b);
 }
 private void runSearch() {
  try {
   int ram = (Integer) ramBox.getSelectedItem(); int price = Integer.parseInt(maxPrice.getText().trim());
   if (price < 0) throw new NumberFormatException();
   String selected = (String) brandBox.getSelectedItem();
   List<Laptop> rows = repository.search("Semua brand".equals(selected) ? "" : selected, ram, price);
   tableModel.setRowCount(0); NumberFormat nf = NumberFormat.getNumberInstance(new Locale("id","ID"));
   for (Laptop l : rows) tableModel.addRow(new Object[]{l.model(),l.brand(),l.processor(),l.ramGb(),
    l.storageGb()+" "+l.storageType(),"Rp "+nf.format(l.priceIdr()),l.screenInch()+" inch",l.gpu(),l.operatingSystem()});
   setTitle("Semantic Laptop Explorer — "+rows.size()+" hasil");
  } catch (NumberFormatException ex) {
   JOptionPane.showMessageDialog(this,"Harga maksimum harus berupa angka Rupiah tanpa titik/koma.","Input tidak valid",JOptionPane.WARNING_MESSAGE);
  } catch (Exception ex) {
   JOptionPane.showMessageDialog(this,"Query gagal: "+ex.getMessage(),"Kesalahan",JOptionPane.ERROR_MESSAGE);
  }
 }
}
