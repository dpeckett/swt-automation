import net from 'net';

const PORT = 9000;

class SWTRobotClient {
  constructor(port) {
    this.port = port;
    this.client = new net.Socket();
  }

  connect() {
    return new Promise((resolve, reject) => {
      this.client.connect(this.port, 'localhost', () => {
        console.log('Connected to SWTRobotAgent');
        resolve();
      });

      this.client.on('error', (error) => {
        console.error('Connection error:', error.message);
        reject(error);
      });

      this.client.on('close', () => {
        console.log('Connection closed');
      });
    });
  }

  sendCommand(command) {
    return new Promise((resolve, reject) => {
      this.client.write(`${command}\n`);

      this.client.once('data', (data) => {
        const response = data.toString().trim();
        console.log(`Response from SWTRobotAgent: ${response}`);

        if (response === 'OK') {
          resolve(response);
        } else {
          console.error('Error:', response);
          reject(new Error(response));
        }
      });
    });
  }

  async clickButton(buttonLabel) {
    try {
      console.log(`Sending command to click button: ${buttonLabel}`);
      await this.sendCommand(`CLICK_BUTTON ${buttonLabel}`);
      console.log('Button clicked successfully');
    } catch (error) {
      console.error(`Failed to click button: ${error.message}`);
      throw error;
    }
  }

  async switchShell(shellTitle) {
    try {
      console.log(`Sending command to switch shell: ${shellTitle}`);
      await this.sendCommand(`SWITCH_SHELL ${shellTitle}`);
      console.log(`Switched to shell '${shellTitle}' successfully`);
    } catch (error) {
      console.error(`Failed to switch shell: ${error.message}`);
      throw error;
    }
  }

  disconnect() {
    this.client.end();
  }
}

// Example usage of the enhanced client
async function runTest() {
  const client = new SWTRobotClient(PORT);

  try {
    await client.connect();

    // Switch to the desired shell
    await client.switchShell('Demo');

    // Click the button in the selected shell
    await client.clickButton('Button');

    console.log('Test completed successfully');
  } catch (error) {
    console.error('Test failed:', error.message);
  } finally {
    client.disconnect();
  }
}

// Execute the test
runTest();
