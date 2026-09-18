import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import multer from 'multer';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import fs from 'fs/promises';
import pkg from 'whatsapp-web.js';
const { Client, LocalAuth, MessageMedia } = pkg;
import qrcode from 'qrcode-terminal';
import moment from 'moment';
import sharp from 'sharp';
import chalk from 'chalk';

const __dirname = dirname(fileURLToPath(import.meta.url));
const app = express();
const port = process.env.PORT || 2222;

app.use(cors());

app.use(express.json({ 
  limit: '50mb',
  extended: true,
  parameterLimit: 50000,
  verify: (req, res, buf, encoding) => {
    req.rawBody = buf.toString(encoding);
  }
}));

app.use(express.urlencoded({ 
  limit: '50mb', 
  extended: true,
  parameterLimit: 50000
}));

app.use((req, res, next) => {
  req.setTimeout(600000);
  res.setTimeout(600000);
  next();
});

app.use((err, req, res, next) => {
  console.error('Global error:', err);
  if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
    return res.status(400).json({ 
      error: 'Invalid JSON',
      details: 'The request body contains invalid JSON'
    });
  }
  next(err);
});

class MessageQueueManager {
  constructor() {
    this.queue = [];
    this.isProcessing = false;
    this.retryAttempts = 3;
    this.retryDelay = 5000; // 5 seconds
  }

  async addMessage(message) {
    this.queue.push({ ...message, retries: 0 });
    if (!this.isProcessing) {
      this.processQueue();
    }
  }

  async processQueue() {
    if (this.isProcessing || this.queue.length === 0) return;
    this.isProcessing = true;
    try {
      const message = this.queue[0];
      const { phoneNumber, contactName, messageText, imageBuffer, retries } = message;
      
      const typingTime = Math.floor(Math.random() * (8000 - 4000 + 1)) + 4000;
      await new Promise(resolve => setTimeout(resolve, typingTime));
      
      try {
        const chatId = phoneNumber + '@c.us';
        
        // Add delay before sending to ensure chat is loaded
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        if (imageBuffer) {
          try {
            const media = new MessageMedia('image/jpeg', imageBuffer, 'visitor.jpg');
            await whatsappClient.sendMessage(chatId, media);
            // Add delay between media and text message
            await new Promise(resolve => setTimeout(resolve, 2000));
          } catch (imageError) {
            console.log(chalk.yellow(`⚠️  Image send failed for ${phoneNumber}, continuing with text only`));
          }
        }
        
        const greeting = contactName ? `Hey, ${contactName}` : "Hey";
        await whatsappClient.sendMessage(chatId, `${greeting}\n\n- New Visitor Alert!\n\n${messageText}`);
        
        console.log(chalk.green(`✅ Message sent successfully to ${phoneNumber}`));
      } catch (error) {
        // Check if it's a WhatsApp-specific error that might be recoverable
        const isWhatsAppError = error.message && (
          error.message.includes('markedUnread') || 
          error.message.includes('Cannot read properties') ||
          error.message.includes('Evaluation failed')
        );
        
        if (isWhatsAppError && retries < this.retryAttempts) {
          console.log(chalk.yellow(`⚠️  Retrying message to ${phoneNumber} (Attempt ${retries + 1}/${this.retryAttempts})`));
          // Re-add message to queue with incremented retry count
          this.queue[0].retries = retries + 1;
          this.queue.push(this.queue.shift());
          
          // Wait before retrying
          await new Promise(resolve => setTimeout(resolve, this.retryDelay));
        } else {
          console.log(chalk.red(`❌ Failed to send message to ${phoneNumber}`));
          if (retries >= this.retryAttempts) {
            console.log(chalk.red(`   Max retries (${this.retryAttempts}) exceeded. Skipping this message.`));
          }
          console.error(chalk.dim(error.message));
          // Remove from queue after max retries
          this.queue.shift();
        }
      }
      
      // Only remove from queue if not already moved for retry
      if (message.retries === this.queue[0]?.retries) {
        this.queue.shift();
      }
      
      if (this.queue.length > 0) {
        const delay = Math.floor(Math.random() * (20000 - 10000 + 1)) + 10000;
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    } catch (error) {
      console.log(chalk.red('❌ Error processing message:'));
      console.error(error);
    }
    this.isProcessing = false;
    if (this.queue.length > 0) {
      this.processQueue();
    }
  }
}
const messageQueue = new MessageQueueManager();

const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, join(__dirname, '../uploads'));
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, uniqueSuffix + '-' + file.originalname);
  }
});

const upload = multer({ 
  storage: storage,
  limits: {
    fileSize: 50 * 1024 * 1024,
    fieldSize: 50 * 1024 * 1024, 
    files: 1
  }
});

const whatsappClient = new Client({
    authStrategy: new LocalAuth(),
    puppeteer: {
        headless: true,
        executablePath: process.platform === 'win32' 
            ? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe' 
            : (process.platform === 'darwin' 
                ? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' 
                : '/usr/bin/google-chrome-stable')
    }
});

whatsappClient.on('qr', qr => {
  console.log(chalk.cyan.bold('\n==============================='));
  console.log(chalk.yellow.bold('📱  WhatsApp QR Code Required!'));
  console.log(chalk.cyan('Scan this QR code with WhatsApp to enable the messaging system:'));
  qrcode.generate(qr, { small: true });
  console.log(chalk.cyan.bold('===============================\n'));
});

whatsappClient.on('ready', () => {
  console.clear();
  console.log(chalk.green.bold('✅ WhatsApp client is ready!'));
});

whatsappClient.on('authenticated', () => {
  console.log(chalk.green('🔒 WhatsApp client authenticated.'));
});

whatsappClient.on('auth_failure', (msg) => {
  console.log(chalk.red.bold('❌ WhatsApp authentication failed! Please try again.'));
});

whatsappClient.on('disconnected', (reason) => {
  console.log(chalk.red('⚠️  WhatsApp client disconnected:'), chalk.yellow(reason));
});

whatsappClient.on('change_state', (state) => {
  console.log(chalk.blue('🔄 WhatsApp client state changed:'), chalk.magenta(state));
});

whatsappClient.on('error', (err) => {
  console.log(chalk.bgRed.white('💥 WhatsApp client error!'));
  console.error(err);
  process.exit(1);
});

app.post('/api/force-qr', async (req, res) => {
  try {
    const authPath = join(__dirname, '../.wwebjs_auth');
    await fs.rm(authPath, { recursive: true, force: true });
    console.log('Authentication cache cleared. Restarting WhatsApp client...');
    whatsappClient.destroy();
    setTimeout(() => whatsappClient.initialize(), 2000);
    res.json({ message: 'Session cleared. Please check terminal for new QR code.' });
  } catch (err) {
    console.error('Failed to clear session:', err);
    res.status(500).json({ error: 'Failed to clear session', details: err.message });
  }
});

async function loadFlatContacts() {
  const data = await fs.readFile(join(__dirname, '../data/flat-contacts.json'), 'utf8');
  return JSON.parse(data);
}

async function fixImageOrientation(imageBuffer) {
  try {
    const rotatedBuffer = await sharp(imageBuffer)
      .rotate()
      .withMetadata() 
      .toBuffer();
    
    return rotatedBuffer;
  } catch (error) {
    console.error('Error fixing image orientation:', error);
    return imageBuffer;
  }
}

async function ensureDirectoryExists(dirPath) {
  try {
    await fs.access(dirPath);
  } catch {
    await fs.mkdir(dirPath, { recursive: true });
  }
}

async function getMonthlyVisitorData(year, month) {
  const yearDir = join(__dirname, '../data', year.toString());
  const monthDir = join(yearDir, month);
  const imagesDir = join(monthDir, 'Images');
  const dataFile = join(monthDir, `${month.toLowerCase()}.json`);

  await ensureDirectoryExists(yearDir);
  await ensureDirectoryExists(monthDir);
  await ensureDirectoryExists(imagesDir);

  try {
    const data = await fs.readFile(dataFile, 'utf8');
    return JSON.parse(data);
  } catch {
    return [];
  }
}

async function saveMonthlyVisitorData(year, month, data) {
  const monthDir = join(__dirname, '../data', year.toString(), month);
  const dataFile = join(monthDir, `${month.toLowerCase()}.json`);
  await fs.writeFile(dataFile, JSON.stringify(data, null, 2));
}

app.post('/api/visitor', async (req, res) => {
  let imageData;
  let imagePath;
  let uniqueSuffix;
  try {
    //console.log(chalk.redBright('➡️  Visitor POST request received'));
    const now = moment();
    const year = now.format('YYYY');
    const month = now.format('MMMM');

    uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    
    const imageDir = join(__dirname, '../data', year, month, 'Images');
    await ensureDirectoryExists(imageDir);
    
    if (req.body && req.body.image) {
      //console.log('Processing base64 image data');
      const base64Data = req.body.image.replace(/^data:image\/\w+;base64,/, '');
      imageData = Buffer.from(base64Data, 'base64');
      
      imageData = await fixImageOrientation(imageData);
      
      imagePath = join(imageDir, uniqueSuffix + '-visitor.jpg');
      
      await fs.writeFile(imagePath, imageData);
    } else {
      //console.log('Processing multipart form upload');
      await new Promise((resolve, reject) => {
        upload.single('image')(req, res, (err) => {
          if (err) {
            console.error('Upload error:', err);
            reject(err);
            return;
          }
          if (!req.file) {
            reject(new Error('No image file uploaded'));
            return;
          }
          resolve();
        });
      });
      
      const originalBuffer = await fs.readFile(req.file.path);
      imageData = await fixImageOrientation(originalBuffer);
      
      imagePath = join(imageDir, uniqueSuffix + '-visitor.jpg');
      await fs.writeFile(imagePath, imageData);
    }

    const { name, flatNumbers, phoneNumber } = req.body;
    if (!name || !flatNumbers || !phoneNumber) {
      throw new Error('Missing required fields: name, flatNumbers, and phoneNumber');
    }
    console.log(chalk.white('------------------------------------------------------------'));
    console.log(chalk.greenBright(`--> New Visitor: ${name}, ${flatNumbers}, ${phoneNumber}`));
    const timestamp = moment().format('MMMM Do YYYY, h:mm:ss a');

    const visitorData = {
      name,
      flatNumbers,
      phoneNumber,
      timestamp,
      imagePath: imagePath.replace(/^.*[\\\/]/, '')
    };

    //console.log('Loading flat contacts...');
    let flatContacts;
    try {
      flatContacts = await loadFlatContacts();
    } catch (error) {
      console.error('Error loading flat contacts:', error);
      throw new Error('Failed to load flat contacts');
    }
    
    const flats = flatNumbers.split(',').map(f => f.trim());
    const contacts = new Set();
    
    let validFlats = false;
    flats.forEach(flat => {
      if (flatContacts[flat]) {
        validFlats = true;
        flatContacts[flat].forEach(contact => {
          contacts.add({
            number: contact.number || contact,
            name: contact.name
          });
        });
      }
    });

    if (!validFlats) {
      throw new Error('No valid flat numbers found in the contacts list');
    }

    if (contacts.size === 0) {
      throw new Error('No contacts found for the provided flat numbers');
    }

    const base64Image = imageData.toString('base64');

    await Promise.all([
      (async () => {
        for (const contact of contacts) {
          const message = `> Name: ${name}\n> Phone: ${phoneNumber}\n> Visiting: ${flatNumbers}\n> Time: ${timestamp}`;
          await messageQueue.addMessage({
            phoneNumber: contact.number,
            contactName: contact.name,
            messageText: message,
            imageBuffer: base64Image
          });
        }
      })(),
      (async () => {
        const visitors = await getMonthlyVisitorData(year, month);
        visitors.push(visitorData);
        await saveMonthlyVisitorData(year, month, visitors);
      })()
    ]);
    res.json({
      message: 'Visitor added successfully',
      visitor: visitorData
    });
  } catch (error) {
    console.log(chalk.red('❌ Error processing visitor:'));
    console.error(error);
    res.status(500).json({ 
      error: 'Failed to process visitor',
      details: error.message 
    });
  }
});

app.get('/api/visitors', async (req, res) => {
  try {
    const { year, month } = req.query;
    
    if (year && month) {
      const visitors = await getMonthlyVisitorData(year, month);
      res.json(visitors);
    } else if (year) {
      const monthDirs = await fs.readdir(join(__dirname, '../data', year));
      const allVisitors = [];
      
      for (const month of monthDirs) {
        if ((await fs.stat(join(__dirname, '../data', year, month))).isDirectory()) {
          const monthData = await getMonthlyVisitorData(year, month);
          allVisitors.push(...monthData);
        }
      }
      
      res.json(allVisitors);
    } else {
      const now = moment();
      const visitors = await getMonthlyVisitorData(
        now.format('YYYY'),
        now.format('MMMM')
      );
      res.json(visitors);
    }
  } catch (error) {
    console.error('Error fetching visitors:', error);
    res.json([]);
  }
});

app.get('/api/health', (req, res) => {
  res.status(200).send();
});

app.use('/uploads', express.static(join(__dirname, '../uploads')));

// Serve website frontend
app.use(express.static(join(__dirname, '../../website')));

// Serve data folder for images
app.use('/data', express.static(join(__dirname, '../data')));

// Fallback to index.html for SPA routing
app.get(/./, (req, res) => {
  res.sendFile(join(__dirname, '../../website/index.html'));
});

async function createDirectories() {
  const dirs = ['uploads', 'data', '.wwebjs_auth'];
  for (const dir of dirs) {
    await fs.mkdir(join(__dirname, '..', dir), { recursive: true });
  }
}

createDirectories().then(async () => {
  const authPath = join(__dirname, '../.wwebjs_auth');
  let sessionExists = false;
  try {
    const files = await fs.readdir(authPath);
    if (files.length > 0) {
      sessionExists = true;
      console.log('WhatsApp session found. Attempting to restore session...');
    } else {
      console.warn('WhatsApp session folder is empty. QR code will be generated.');
    }
  } catch {
    console.warn('WhatsApp session folder not found. QR code will be generated.');
  }

  app.listen(port, () => {
    console.log(chalk.bgGreen.black.bold('🚀 Server is running!'));
    console.log(chalk.green(`Listening on port ${port}`));
    console.log(chalk.blue('Initializing WhatsApp client...'));
    whatsappClient.initialize().catch((err) => {
      console.log(chalk.bgRed.white('💥 Error initializing WhatsApp client!'));
      console.error(err);
      process.exit(1);
    });
  });
});