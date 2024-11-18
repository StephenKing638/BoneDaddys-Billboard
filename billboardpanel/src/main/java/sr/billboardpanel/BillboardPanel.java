package sr.billboardpanel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.imageio.ImageIO;

public class BillboardPanel extends JFrame {

    float maxDim = 540;
    Alignment alignment = Alignment.BOTTOM_RIGHT;

    enum Alignment {
        TOP_LEFT {
            @Override
            public void setLocation(JFrame f, Rectangle bounds) {
                f.setLocation(0, 0);
            }
        },
        BOTTOM_LEFT {
            @Override
            public void setLocation(JFrame f, Rectangle bounds) {
                f.setLocation(0, bounds.height - f.getHeight());
            }
        },
        TOP_RIGHT {
            @Override
            public void setLocation(JFrame f, Rectangle bounds) {
                f.setLocation(bounds.width - f.getWidth(), 0);
            }
        },
        BOTTOM_RIGHT {
            @Override
            public void setLocation(JFrame f, Rectangle bounds) {
                f.setLocation(bounds.width - f.getWidth(), bounds.height - f.getHeight());
            }
        };

        public abstract void setLocation(JFrame f, Rectangle bounds);
    }

    volatile boolean allowMovement = false;

    private Point initialClick;
    private BufferedImage bi;

    float height;
    float width;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new BillboardPanel();
            } catch (IOException e) {
                printCrash(e);
                System.exit(-1);
            }
        });
    }

    public BillboardPanel() throws IOException {
        setTitle("Billboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setAlwaysOnTop(true);

        File f = new File("maxdim.txt");
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            maxDim = Math.abs(Float.valueOf(reader.readLine()));
            alignment = Alignment.valueOf(reader.readLine());
        } catch(Throwable e) {
            try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
                writer.write(maxDim + "\n");
                writer.write(alignment.name());
            }
        }

        // Load the image to render
        try {
            bi = ImageIO.read(new File("image.png"));
            if(bi == null) throw new IOException();
        } catch(IOException e) {
            throw new IOException("No 'image.png' file was found or could not be read, make sure you have the image file in the same folder as the .exe and that it is readable");
        }

        // Create a custom JPanel to display the image
        JPanel imagePanel = new JPanel() {

            Color transparent = new Color(0, 0, 0, 0);

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setBackground(transparent);
                super.paintComponent(g);

                if (bi != null) {
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.scale(width / bi.getWidth(), height / bi.getHeight());
                    g2.drawImage(bi, 0, 0, null);
                }
            }
        };
        imagePanel.setOpaque(false);

        setBackground(new Color(0, 0, 0, 0));

        float scaleFactor = Math.min(maxDim / bi.getWidth(), maxDim / bi.getHeight());

        width = bi.getWidth() * scaleFactor;
        height = bi.getHeight() * scaleFactor;

        // Set panel size to match the scaled dimensions
        imagePanel.setPreferredSize(new Dimension((int) width, (int) height));
        add(imagePanel);
        pack();

        // set pos
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = ge.getMaximumWindowBounds();
        alignment.setLocation(this, bounds);
        this.x = getX();
        this.y = getY();

        // Add ESC key listener to close the window
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                switch(e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE: {
                        dispose();
                        System.exit(0);
                        break;
                    }
                    case KeyEvent.VK_F1: {
                        allowMovement = !allowMovement;
                        break;
                    }
                    case KeyEvent.VK_F2: {
                        if(task.isRunning()) {
                            task.stop();
                        } else {
                            task.start();
                        }
                        break;
                    }
                    default : break;
                }
            }
        });

        // mouse dragging
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if(!allowMovement) {
                    return;
                }

                // Get the current location of the JFrame
                int thisX = getLocation().x;
                int thisY = getLocation().y;

                // Determine the new location
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                int newX = thisX + xMoved;
                int newY = thisY + yMoved;

                // Move the JFrame to the new location
                setLocation(newX, newY);

                x = newX;
                y = newY;
            }
        });

        setVisible(true);
    }


    volatile double degRot = -45;
    private float randomFactor = 15;
    private float fps = 60;
    private float vel = 120; // 30 pixels per sec

    private float x;
    private float y;

    private Timer task = new Timer(1000 / (int) fps, (e) -> {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle windowBounds = ge.getMaximumWindowBounds();
        Rectangle bounds = getBounds();
    
        float vel = this.vel / fps;
        double radRot = Math.toRadians(degRot);
        
        x += (float) (vel * Math.cos(radRot));
        y += (float) (vel * Math.sin(radRot));
    
        // Check for collision with screen boundaries and adjust direction
        if (x < windowBounds.x) {
            x = windowBounds.x; // clamp
            degRot = 180 - degRot + randomFactor * (Math.random() - 0.5);
        } else if (x + bounds.width > windowBounds.x + windowBounds.width) {
            x = windowBounds.x + windowBounds.width - bounds.width; // clamp
            degRot = 180 - degRot + randomFactor * (Math.random() - 0.5);
        }
    
        if (y < windowBounds.y) {
            y = windowBounds.y; // clamp
            degRot = -degRot + randomFactor * (Math.random() - 0.5);
        } else if (y + bounds.height > windowBounds.y + windowBounds.height) {
            y = windowBounds.y + windowBounds.height - bounds.height; // clamp
            degRot = -degRot + randomFactor * (Math.random() - 0.5);
        }
    
        setLocation((int) x, (int) y);
    });

    public static void printCrash(Throwable e) {
        String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("'CRASH-'MMM-dd-yyyy'.log'"));
        File f2 = new File(name);

        try (PrintStream ps = new PrintStream(f2)) {
            ps.println("App failed to launch, see the exception below to resolve");
            e.printStackTrace(ps);
        } catch (IOException e1) {
            e.printStackTrace();
        }
    }
}
